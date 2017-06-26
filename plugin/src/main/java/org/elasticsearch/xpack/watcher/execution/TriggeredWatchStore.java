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

package org.elasticsearch.xpack.watcher.execution;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.indices.TypeMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.watch.WatchStoreUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;

public class TriggeredWatchStore extends AbstractComponent {

    public static final String INDEX_NAME = ".triggered_watches";
    public static final String DOC_TYPE = "doc";
    public static final String LEGACY_DOC_TYPE = "triggered_watch";

    private final int scrollSize;
    private final WatcherClientProxy client;
    private final TimeValue scrollTimeout;
    private final TriggeredWatch.Parser triggeredWatchParser;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final TimeValue defaultBulkTimeout;

    @Inject
    public TriggeredWatchStore(Settings settings, InternalClient client, TriggeredWatch.Parser triggeredWatchParser) {
        this(settings, new WatcherClientProxy(settings, client), triggeredWatchParser);
    }

    public TriggeredWatchStore(Settings settings, WatcherClientProxy client, TriggeredWatch.Parser triggeredWatchParser) {
        super(settings);
        this.scrollSize = settings.getAsInt("xpack.watcher.execution.scroll.size", 100);
        this.client = client;
        this.scrollTimeout = settings.getAsTime("xpack.watcher.execution.scroll.timeout", TimeValue.timeValueSeconds(30));
        this.triggeredWatchParser = triggeredWatchParser;
        this.defaultBulkTimeout = settings.getAsTime("xpack.watcher.internal.ops.bulk.default_timeout", TimeValue.timeValueSeconds(120));
    }

    public void start() {
        started.set(true);
    }

    public boolean validate(ClusterState state) {
        try {
            IndexMetaData indexMetaData = WatchStoreUtils.getConcreteIndex(INDEX_NAME, state.metaData());
            if (indexMetaData.getState() == IndexMetaData.State.CLOSE) {
                logger.debug("triggered watch index [{}] is marked as closed, watcher cannot be started",
                        indexMetaData.getIndex().getName());
                return false;
            } else {
                return state.routingTable().index(indexMetaData.getIndex()).allPrimaryShardsActive();
            }
        // no index exists, so we can start
        } catch (IndexNotFoundException e) {
            return true;
        } catch (IllegalStateException e) {
            logger.trace((Supplier<?>) () -> new ParameterizedMessage("error getting index meta data [{}]: ", INDEX_NAME), e);
            return false;
        }
    }

    public void stop() {
        started.set(false);
    }

    public void putAll(final List<TriggeredWatch> triggeredWatches, final ActionListener<BulkResponse> listener) throws IOException {
        if (triggeredWatches.isEmpty()) {
            listener.onResponse(new BulkResponse(new BulkItemResponse[]{}, 0));
            return;
        }

        ensureStarted();
        // have a read lock to tell the rw-lock that there is a current access, so that stopping waits until all write operations
        // are finished. This also means, that the lock requires unlock on all cases, no matter if success or failure
        BulkRequest request = createBulkRequest(triggeredWatches, DOC_TYPE);
        client.bulk(request, ActionListener.wrap(r -> {
            if (containsTypeMissingException(r)) {
                client.bulk(createBulkRequest(triggeredWatches, LEGACY_DOC_TYPE), listener);
            } else {
                listener.onResponse(r);
            }
        }, listener::onFailure));
    }

    public BulkResponse putAll(final List<TriggeredWatch> triggeredWatches) throws IOException {
        PlainActionFuture<BulkResponse> future = PlainActionFuture.newFuture();
        putAll(triggeredWatches, future);
        return future.actionGet(defaultBulkTimeout);
    }

    /**
     * Check if there is any type missing exception in the response
     * @param response  The BulkResponse to check for
     * @return          true if any bulk response item contains the above mentioned exception
     */
    private boolean containsTypeMissingException(BulkResponse response) {
        return response.hasFailures() && Arrays.stream(response.getItems()).anyMatch(item -> item.isFailed() && ExceptionsHelper
                .unwrapCause(item.getFailure().getCause()) instanceof TypeMissingException);
    }

    /**
     * Create a bulk request from the triggered watches with a specified document type
     * @param triggeredWatches  The list of triggered watches
     * @param docType           The document type to use, either the current one or legacy
     * @return                  The bulk request for the triggered watches
     * @throws IOException      If a triggered watch could not be parsed to JSON, this exception is thrown
     */
    private BulkRequest createBulkRequest(final List<TriggeredWatch> triggeredWatches, String docType) throws IOException {
        BulkRequest request = new BulkRequest();
        for (TriggeredWatch triggeredWatch : triggeredWatches) {
            IndexRequest indexRequest = new IndexRequest(INDEX_NAME, docType, triggeredWatch.id().value());
            try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                triggeredWatch.toXContent(builder, ToXContent.EMPTY_PARAMS);
                indexRequest.source(builder);
            }
            indexRequest.opType(IndexRequest.OpType.CREATE);
            request.add(indexRequest);
        }
        return request;
    }

    public void delete(Wid wid) {
        ensureStarted();
        DeleteResponse response = client.delete(new DeleteRequest(INDEX_NAME, DOC_TYPE, wid.value()));
        if (response.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            client.delete(new DeleteRequest(INDEX_NAME, LEGACY_DOC_TYPE, wid.value()));
        }
        logger.trace("successfully deleted triggered watch with id [{}]", wid);
    }

    Collection<TriggeredWatch> loadTriggeredWatches(ClusterState state) {
        IndexMetaData indexMetaData;
        try {
            indexMetaData = WatchStoreUtils.getConcreteIndex(INDEX_NAME, state.metaData());
        } catch (IndexNotFoundException e) {
            return Collections.emptySet();
        }

        int numPrimaryShards;
        if (state.routingTable().index(indexMetaData.getIndex()).allPrimaryShardsActive() == false) {
            throw illegalState("not all primary shards of the triggered watches index {} are started", indexMetaData.getIndex());
        } else {
            numPrimaryShards = indexMetaData.getNumberOfShards();
        }
        RefreshResponse refreshResponse = client.refresh(new RefreshRequest(INDEX_NAME));
        if (refreshResponse.getSuccessfulShards() < numPrimaryShards) {
            throw illegalState("refresh was supposed to run on [{}] shards, but ran on [{}] shards", numPrimaryShards,
                    refreshResponse.getSuccessfulShards());
        }

        SearchRequest searchRequest = createScanSearchRequest();
        SearchResponse response = client.search(searchRequest, null);
        List<TriggeredWatch> triggeredWatches = new ArrayList<>();
        try {
            if (response.getTotalShards() != response.getSuccessfulShards()) {
                throw illegalState("scan search was supposed to run on [{}] shards, but ran on [{}] shards", numPrimaryShards,
                        response.getSuccessfulShards());
            }

            while (response.getHits().getHits().length != 0) {
                for (SearchHit sh : response.getHits()) {
                    String id = sh.getId();
                    try {
                        TriggeredWatch triggeredWatch = triggeredWatchParser.parse(id, sh.getVersion(), sh.getSourceRef());
                        logger.trace("loaded triggered watch [{}/{}/{}]", sh.getIndex(), sh.getType(), sh.getId());
                        triggeredWatches.add(triggeredWatch);
                    } catch (Exception e) {
                        logger.error(
                                (Supplier<?>) () -> new ParameterizedMessage("couldn't load triggered watch [{}], ignoring it...", id), e);
                    }
                }
                response = client.searchScroll(response.getScrollId(), scrollTimeout);
            }
        } finally {
            client.clearScroll(response.getScrollId());
        }
        logger.debug("loaded [{}] triggered watches", triggeredWatches.size());
        return triggeredWatches;
    }

    private SearchRequest createScanSearchRequest() {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .size(scrollSize)
                .sort(SortBuilders.fieldSort("_doc"));

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(sourceBuilder);
        searchRequest.scroll(scrollTimeout);
        searchRequest.preference("_primary");
        return searchRequest;
    }

    private void ensureStarted() {
        if (!started.get()) {
            throw illegalState("unable to persist triggered watches, the store is not ready");
        }
    }

}
