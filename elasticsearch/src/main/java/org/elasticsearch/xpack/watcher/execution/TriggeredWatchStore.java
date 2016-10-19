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
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.watch.WatchStoreUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;
import static org.elasticsearch.xpack.watcher.support.Exceptions.ioException;

public class TriggeredWatchStore extends AbstractComponent {

    public static final String INDEX_NAME = ".triggered_watches";
    public static final String DOC_TYPE = "triggered_watch";

    private final int scrollSize;
    private final WatcherClientProxy client;
    private final TimeValue scrollTimeout;
    private final TriggeredWatch.Parser triggeredWatchParser;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock accessLock = readWriteLock.readLock();
    private final Lock stopLock = readWriteLock.writeLock();
    private final AtomicBoolean started = new AtomicBoolean(false);

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
    }

    public void start() {
        started.set(true);
    }

    public boolean validate(ClusterState state) {
        try {
            IndexMetaData indexMetaData = WatchStoreUtils.getConcreteIndex(INDEX_NAME, state.metaData());
            return state.routingTable().index(indexMetaData.getIndex()).allPrimaryShardsActive();
        } catch (IndexNotFoundException e) {
        } catch (IllegalStateException e) {
            logger.trace((Supplier<?>) () -> new ParameterizedMessage("error getting index meta data [{}]: ", INDEX_NAME), e);
            return false;
        }
        return true;
    }

    public void stop() {
        stopLock.lock(); // This will block while put or update actions are underway
        try {
            started.set(false);
        } finally {
            stopLock.unlock();
        }
    }

    public void put(TriggeredWatch triggeredWatch) throws Exception {
        ensureStarted();
        accessLock.lock();
        try {
            IndexRequest request = new IndexRequest(INDEX_NAME, DOC_TYPE, triggeredWatch.id().value())
                    .source(XContentFactory.jsonBuilder().value(triggeredWatch))
                    .opType(IndexRequest.OpType.CREATE);
            client.index(request, (TimeValue) null);
        } catch (IOException e) {
            throw ioException("failed to persist triggered watch [{}]", e, triggeredWatch);
        } finally {
            accessLock.unlock();
        }
    }

    public void put(final TriggeredWatch triggeredWatch, final ActionListener<Boolean> listener) throws Exception {
        ensureStarted();
        try {
            IndexRequest request = new IndexRequest(INDEX_NAME, DOC_TYPE, triggeredWatch.id().value())
                    .source(XContentFactory.jsonBuilder().value(triggeredWatch))
                    .opType(IndexRequest.OpType.CREATE);
            client.index(request, new ActionListener<IndexResponse>() {
                @Override
                public void onResponse(IndexResponse response) {
                    listener.onResponse(true);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } catch (IOException e) {
            throw ioException("failed to persist triggered watch [{}]", e, triggeredWatch);
        }
    }

    public void putAll(final List<TriggeredWatch> triggeredWatches, final ActionListener<List<Integer>> listener) throws Exception {

        if (triggeredWatches.isEmpty()) {
            listener.onResponse(Collections.emptyList());
            return;
        }

        if (triggeredWatches.size() == 1) {
            put(triggeredWatches.get(0), new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean success) {
                    listener.onResponse(Collections.singletonList(0));
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
            return;
        }

        ensureStarted();
        try {
            BulkRequest request = new BulkRequest();
            for (TriggeredWatch triggeredWatch : triggeredWatches) {
                IndexRequest indexRequest = new IndexRequest(INDEX_NAME, DOC_TYPE, triggeredWatch.id().value());
                indexRequest.source(XContentFactory.jsonBuilder().value(triggeredWatch));
                indexRequest.opType(IndexRequest.OpType.CREATE);
                request.add(indexRequest);
            }
            client.bulk(request, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse response) {
                    List<Integer> successFullSlots = new ArrayList<Integer>();
                    for (int i = 0; i < response.getItems().length; i++) {
                        BulkItemResponse itemResponse = response.getItems()[i];
                        if (itemResponse.isFailed()) {
                            logger.error("could store triggered watch with id [{}], because failed [{}]", itemResponse.getId(),
                                    itemResponse.getFailureMessage());
                        } else {
                            IndexResponse indexResponse = itemResponse.getResponse();
                            successFullSlots.add(i);
                        }
                    }
                    listener.onResponse(successFullSlots);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } catch (IOException e) {
            throw ioException("failed to persist triggered watches", e);
        }
    }

    public List<Integer> putAll(final List<TriggeredWatch> triggeredWatches) throws Exception {
        ensureStarted();
        try {
            BulkRequest request = new BulkRequest();
            for (TriggeredWatch triggeredWatch : triggeredWatches) {
                IndexRequest indexRequest = new IndexRequest(INDEX_NAME, DOC_TYPE, triggeredWatch.id().value());
                indexRequest.source(XContentFactory.jsonBuilder().value(triggeredWatch));
                indexRequest.opType(IndexRequest.OpType.CREATE);
                request.add(indexRequest);
            }
            BulkResponse response = client.bulk(request, (TimeValue) null);
            List<Integer> successFullSlots = new ArrayList<>();
            for (int i = 0; i < response.getItems().length; i++) {
                BulkItemResponse itemResponse = response.getItems()[i];
                if (itemResponse.isFailed()) {
                    logger.error("could store triggered watch with id [{}], because failed [{}]", itemResponse.getId(),
                            itemResponse.getFailureMessage());
                } else {
                    IndexResponse indexResponse = itemResponse.getResponse();
                    successFullSlots.add(i);
                }
            }
            return successFullSlots;
        } catch (IOException e) {
            throw ioException("failed to persist triggered watches", e);
        }
    }

    public void delete(Wid wid) {
        ensureStarted();
        accessLock.lock();
        try {
            DeleteRequest request = new DeleteRequest(INDEX_NAME, DOC_TYPE, wid.value());
            client.delete(request);
            logger.trace("successfully deleted triggered watch with id [{}]", wid);
        } finally {
            accessLock.unlock();
        }
    }

    public Collection<TriggeredWatch> loadTriggeredWatches(ClusterState state) {
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

            while (response.getHits().hits().length != 0) {
                for (SearchHit sh : response.getHits()) {
                    String id = sh.getId();
                    try {
                        TriggeredWatch triggeredWatch = triggeredWatchParser.parse(id, sh.version(), sh.getSourceRef());
                        logger.trace("loaded triggered watch [{}/{}/{}]", sh.index(), sh.type(), sh.id());
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
        searchRequest.types(DOC_TYPE);
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
