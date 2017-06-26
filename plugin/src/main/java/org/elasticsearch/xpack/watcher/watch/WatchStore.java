/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.watcher.watch;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.indices.TypeMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.xpack.common.stats.Counters;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.actions.ActionWrapper;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.xpack.watcher.trigger.schedule.Schedule;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTrigger;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;

public class WatchStore extends AbstractComponent {

    public static final String INDEX = ".watches";
    public static final String DOC_TYPE = "doc";
    public static final String LEGACY_DOC_TYPE = "watch";

    private final WatcherClientProxy client;
    private final Watch.Parser watchParser;

    private final ConcurrentMap<String, Watch> watches;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final int scrollSize;
    private final TimeValue scrollTimeout;

    @Inject
    public WatchStore(Settings settings, InternalClient client, Watch.Parser watchParser) {
        this(settings, new WatcherClientProxy(settings, client), watchParser);
    }

    public WatchStore(Settings settings, WatcherClientProxy client, Watch.Parser watchParser) {
        super(settings);
        this.client = client;
        this.watchParser = watchParser;
        this.watches = ConcurrentCollections.newConcurrentMap();

        this.scrollTimeout = settings.getAsTime("xpack.watcher.watch.scroll.timeout", TimeValue.timeValueSeconds(30));
        this.scrollSize = settings.getAsInt("xpack.watcher.watch.scroll.size", 100);
    }

    public void start(ClusterState state) throws Exception {
        if (started.get()) {
            logger.debug("watch store already started");
            return;
        }

        try {
            IndexMetaData indexMetaData = WatchStoreUtils.getConcreteIndex(INDEX, state.metaData());
            int count = loadWatches(indexMetaData);
            logger.debug("loaded [{}] watches from the watches index [{}]", count, indexMetaData.getIndex().getName());
        } catch (IndexNotFoundException e) {
        } catch (Exception e) {
            logger.debug((Supplier<?>) () -> new ParameterizedMessage("failed to load watches for watch index [{}]", INDEX), e);
            watches.clear();
            throw e;
        }

        started.set(true);
    }

    public boolean validate(ClusterState state) {
        try {
            IndexMetaData indexMetaData = WatchStoreUtils.getConcreteIndex(INDEX, state.metaData());
            if (indexMetaData.getState() == IndexMetaData.State.CLOSE) {
                logger.debug("watch index [{}] is marked as closed, watcher cannot be started",
                        indexMetaData.getIndex().getName());
                return false;
            } else {
                return state.routingTable().index(indexMetaData.getIndex()).allPrimaryShardsActive();
            }
        } catch (IndexNotFoundException e) {
            return true;
        } catch (IllegalStateException e) {
            logger.trace((Supplier<?>) () -> new ParameterizedMessage("error getting index meta data [{}]: ", INDEX), e);
            return false;
        }
    }

    public boolean started() {
        return started.get();
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            watches.clear();
            logger.info("stopped watch store");
        }
    }

    /**
     * Returns the watch with the specified id otherwise <code>null</code> is returned.
     */
    public Watch get(String id) {
        ensureStarted();
        return watches.get(id);
    }

    /**
     * Creates an watch if this watch already exists it will be overwritten
     */
    public WatchPut put(Watch watch) throws IOException {
        ensureStarted();
        IndexResponse response;
        try {
            IndexRequest indexRequest = createIndexRequest(DOC_TYPE, watch);
            response = client.index(indexRequest, (TimeValue) null);
        } catch (TypeMissingException e) {
            // this exception indicates the watch index was not updated yet and is not using the new doc type, so we try to store
            // with the old 'watch' document type
            IndexRequest indexRequest = createIndexRequest(LEGACY_DOC_TYPE, watch);
            response = client.index(indexRequest, (TimeValue) null);
        }

        watch.status().version(response.getVersion());
        watch.version(response.getVersion());
        Watch previous = watches.put(watch.id(), watch);
        return new WatchPut(previous, watch, response);
    }

    private IndexRequest createIndexRequest(String docType, Watch watch) throws IOException {
        IndexRequest indexRequest = new IndexRequest(INDEX, docType, watch.id());
        boolean useOldStatus = LEGACY_DOC_TYPE.equals(docType);
        BytesReference source = watch.toXContent(jsonBuilder(), WatcherParams.builder().put(Watch.INCLUDE_STATUS_KEY, true)
                .put(Watch.WRITE_STATUS_WITH_UNDERSCORE, useOldStatus).build()).bytes();
        indexRequest.source(BytesReference.toBytes(source), XContentType.JSON);
        indexRequest.version(Versions.MATCH_ANY);
        indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        return indexRequest;
    }

    /**
     * Updates and persists the status of the given watch
     */
    public void updateStatus(Watch watch) throws IOException {
        updateStatus(watch, false);
    }

    /**
     * Updates and persists the status of the given watch
     *
     * at the moment we store the status together with the watch,
     * so we just need to update the watch itself
     */
    public void updateStatus(Watch watch, boolean refresh) throws IOException {
        ensureStarted();
        if (!watch.status().dirty()) {
            return;
        }

        UpdateResponse response = null;
        try {
            try {
                response = client.update(createUpdateRequest(watch, DOC_TYPE, Watch.Field.STATUS, refresh));
            } catch (DocumentMissingException e) {
                // this means the mapping has changed and we can try to write with the old type 'watch'
                // and thus we retry to write the status in the old fashion
                response = client.update(createUpdateRequest(watch, LEGACY_DOC_TYPE, Watch.Field.STATUS_V5, refresh));
            }
        } catch (DocumentMissingException e) {
            // do not rethrow an exception, otherwise the watch history will contain an exception
            // even though the execution might has been fine
            logger.warn("Watch [{}] was deleted during watch execution, not updating watch status", watch.id());
        }

        if (response != null) {
            watch.status().version(response.getVersion());
            watch.version(response.getVersion());
            watch.status().resetDirty();
        }
    }

    private UpdateRequest createUpdateRequest(Watch watch, String type, ParseField statusFieldName, boolean refresh) throws IOException {
        XContentBuilder source = JsonXContent.contentBuilder().
                startObject()
                .field(statusFieldName.getPreferredName(), watch.status(), ToXContent.EMPTY_PARAMS)
                .endObject();

        UpdateRequest updateRequest = new UpdateRequest(INDEX, type, watch.id());
        updateRequest.doc(source);
        updateRequest.version(watch.version());

        if (refresh) {
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        }

        return updateRequest;
    }

    /**
     * Deletes the watch with the specified id if exists
     */
    public WatchDelete delete(String id) {
        ensureStarted();
        Watch watch = watches.remove(id);
        // even if the watch was not found in the watch map, we should still try to delete it
        // from the index, just to make sure we don't leave traces of it
        DeleteRequest request = new DeleteRequest(INDEX, DOC_TYPE, id).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        DeleteResponse response = client.delete(request);
        // if the deletion failed because the document does not exist, try to run another delete operation
        // using the legacy document type - only if both are deleted we can be sure the watch is not persisted anymore during
        // the transition phase
        if (response.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            response = client.delete(new DeleteRequest(INDEX, LEGACY_DOC_TYPE, id).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE));
        }
        // Another operation may hold the Watch instance, so lets set the version for consistency:
        if (watch != null) {
            watch.version(response.getVersion());
        }
        return new WatchDelete(response);
    }

    public Collection<Watch> watches() {
        return watches.values();
    }

    public Collection<Watch> activeWatches() {
        Set<Watch> watches = new HashSet<>();
        for (Watch watch : watches()) {
            if (watch.status().state().isActive()) {
                watches.add(watch);
            }
        }
        return watches;
    }

    public Map<String, Object> usageStats() {
        Counters counters = new Counters("count.total", "count.active");
        for (Watch watch : watches.values()) {
            boolean isActive = watch.status().state().isActive();
            addToCounters("count", isActive, counters);

            // schedule
            if (watch.trigger() != null) {
                addToCounters("watch.trigger._all", isActive, counters);
                if ("schedule".equals(watch.trigger().type())) {
                    Schedule schedule = ((ScheduleTrigger) watch.trigger()).getSchedule();
                    addToCounters("watch.trigger.schedule._all", isActive, counters);
                    addToCounters("watch.trigger.schedule." + schedule.type(), isActive, counters);
                }
            }

            // input
            if (watch.input() != null) {
                String type = watch.input().type();
                addToCounters("watch.input._all", isActive, counters);
                addToCounters("watch.input." + type, isActive, counters);
            }

            // condition
            if (watch.condition() != null) {
                String type = watch.condition().type();
                addToCounters("watch.condition._all", isActive, counters);
                addToCounters("watch.condition." + type, isActive, counters);
            }

            // actions
            for (ActionWrapper actionWrapper : watch.actions()) {
                String type = actionWrapper.action().type();
                addToCounters("watch.action." + type, isActive, counters);
                if (actionWrapper.transform() != null) {
                    String transformType = actionWrapper.transform().type();
                    addToCounters("watch.transform." + transformType, isActive, counters);
                }
            }

            // transform
            if (watch.transform() != null) {
                String type = watch.transform().type();
                addToCounters("watch.transform." + type, isActive, counters);
            }

            // metadata
            if (watch.metadata() != null && watch.metadata().size() > 0) {
                addToCounters("watch.metadata", isActive, counters);
            }
        }

        return counters.toMap();
    }

    private void addToCounters(String name, boolean isActive, Counters counters) {
        counters.inc(name + ".total");
        if (isActive) {
            counters.inc(name + ".active");
        }
    }

    /**
     * scrolls all the watch documents in the watches index, parses them, and loads them into
     * the given map.
     */
    private int loadWatches(IndexMetaData indexMetaData) {
        assert watches.isEmpty() : "no watches should reside, but there are [" + watches.size() + "] watches.";
        RefreshResponse refreshResponse = client.refresh(new RefreshRequest(INDEX));
        if (refreshResponse.getSuccessfulShards() < indexMetaData.getNumberOfShards()) {
            throw illegalState("not all required shards have been refreshed");
        }

        // only try to upgrade the source, if we come from a 2.x index
        boolean upgradeSource = indexMetaData.getCreationVersion().before(Version.V_5_0_0_alpha1);

        int count = 0;
        SearchRequest searchRequest = new SearchRequest(INDEX)
                .preference("_primary")
                .scroll(scrollTimeout)
                .source(new SearchSourceBuilder()
                        .size(scrollSize)
                        .sort(SortBuilders.fieldSort("_doc"))
                        .version(true));
        SearchResponse response = client.search(searchRequest, null);
        try {
            if (response.getTotalShards() != response.getSuccessfulShards()) {
                throw new ElasticsearchException("Partial response while loading watches");
            }

            while (response.getHits().hits().length != 0) {
                for (SearchHit hit : response.getHits()) {
                    String id = hit.getId();
                    try {
                        final BytesReference source = hit.getSourceRef();
                        Watch watch =
                                watchParser.parse(id, true, source, XContentFactory.xContentType(source), upgradeSource);
                        watch.status().version(hit.getVersion());
                        watch.version(hit.getVersion());
                        watches.put(id, watch);
                        count++;
                    } catch (Exception e) {
                        logger.error((Supplier<?>) () -> new ParameterizedMessage("couldn't load watch [{}], ignoring it...", id), e);
                    }
                }
                response = client.searchScroll(response.getScrollId(), scrollTimeout);
            }
        } finally {
            client.clearScroll(response.getScrollId());
        }
        return count;
    }

    private void ensureStarted() {
        if (!started.get()) {
            throw new IllegalStateException("watch store not started");
        }
    }

    public void clearWatchesInMemory() {
        watches.clear();
    }

    public class WatchPut {

        private final Watch previous;
        private final Watch current;
        private final IndexResponse response;

        public WatchPut(Watch previous, Watch current, IndexResponse response) {
            this.current = current;
            this.previous = previous;
            this.response = response;
        }

        public Watch current() {
            return current;
        }

        public Watch previous() {
            return previous;
        }

        public IndexResponse indexResponse() {
            return response;
        }
    }

    public class WatchDelete {

        private final DeleteResponse response;

        public WatchDelete(DeleteResponse response) {
            this.response = response;
        }

        public DeleteResponse deleteResponse() {
            return response;
        }
    }
}
