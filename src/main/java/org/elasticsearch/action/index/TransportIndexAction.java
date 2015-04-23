/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.index;

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.RoutingMissingException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.create.TransportCreateIndexAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.AutoCreateIndex;
import org.elasticsearch.action.support.replication.TransportShardReplicationOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.action.index.MappingUpdatedAction;
import org.elasticsearch.cluster.action.shard.ShardStateAction;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.compress.CompressedString;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.engine.Engine.IndexingOperation;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.Mapping;
import org.elasticsearch.index.mapper.SourceToParse;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.river.RiverIndexName;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performs the index operation.
 * <p/>
 * <p>Allows for the following settings:
 * <ul>
 * <li><b>autoCreateIndex</b>: When set to <tt>true</tt>, will automatically create an index if one does not exists.
 * Defaults to <tt>true</tt>.
 * <li><b>allowIdGeneration</b>: If the id is set not, should it be generated. Defaults to <tt>true</tt>.
 * </ul>
 */
public class TransportIndexAction extends TransportShardReplicationOperationAction<IndexRequest, IndexRequest, IndexResponse> {

    private final AutoCreateIndex autoCreateIndex;

    private final boolean allowIdGeneration;

    private final TransportCreateIndexAction createIndexAction;

    private final MappingUpdatedAction mappingUpdatedAction;

    private final ClusterService clusterService;

    @Inject
    public TransportIndexAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                IndicesService indicesService, ThreadPool threadPool, ShardStateAction shardStateAction,
                                TransportCreateIndexAction createIndexAction, MappingUpdatedAction mappingUpdatedAction, ActionFilters actionFilters) {
        super(settings, IndexAction.NAME, transportService, clusterService, indicesService, threadPool, shardStateAction, actionFilters);
        this.createIndexAction = createIndexAction;
        this.mappingUpdatedAction = mappingUpdatedAction;
        this.autoCreateIndex = new AutoCreateIndex(settings);
        this.allowIdGeneration = settings.getAsBoolean("action.allow_id_generation", true);
        this.clusterService = clusterService;
    }

    @Override
    protected void doExecute(final IndexRequest request, final ActionListener<IndexResponse> listener) {
        // if we don't have a master, we don't have metadata, that's fine, let it find a master using create index API
        if (autoCreateIndex.shouldAutoCreate(request.index(), clusterService.state())) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(request);
            createIndexRequest.index(request.index());
            createIndexRequest.mapping(request.type());
            createIndexRequest.cause("auto(index api)");
            createIndexRequest.masterNodeTimeout(request.timeout());
            createIndexAction.execute(createIndexRequest, new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse result) {
                    innerExecute(request, listener);
                }

                @Override
                public void onFailure(Throwable e) {
                    if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
                        // we have the index, do it
                        try {
                            innerExecute(request, listener);
                        } catch (Throwable e1) {
                            listener.onFailure(e1);
                        }
                    } else {
                        listener.onFailure(e);
                    }
                }
            });
        } else {
            innerExecute(request, listener);
        }
    }

    @Override
    protected boolean resolveIndex() {
        return true;
    }

    @Override
    protected void resolveRequest(ClusterState state, InternalRequest request, ActionListener<IndexResponse> indexResponseActionListener) {
        MetaData metaData = clusterService.state().metaData();

        MappingMetaData mappingMd = null;
        if (metaData.hasIndex(request.concreteIndex())) {
            mappingMd = metaData.index(request.concreteIndex()).mappingOrDefault(request.request().type());
        }
        request.request().process(metaData, mappingMd, allowIdGeneration, request.concreteIndex());
    }

    private void innerExecute(final IndexRequest request, final ActionListener<IndexResponse> listener) {
        super.doExecute(request, listener);
    }

    @Override
    protected boolean checkWriteConsistency() {
        return true;
    }

    @Override
    protected IndexRequest newRequestInstance() {
        return new IndexRequest();
    }

    @Override
    protected IndexRequest newReplicaRequestInstance() {
        return newRequestInstance();
    }

    @Override
    protected IndexResponse newResponseInstance() {
        return new IndexResponse();
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected ShardIterator shards(ClusterState clusterState, InternalRequest request) {
        return clusterService.operationRouting()
                .indexShards(clusterService.state(), request.concreteIndex(), request.request().type(), request.request().id(), request.request().routing());
    }

    @Override
    protected Tuple<IndexResponse, IndexRequest> shardOperationOnPrimary(ClusterState clusterState, PrimaryOperationRequest shardRequest) throws Throwable {
        final IndexRequest request = shardRequest.request;

        // validate, if routing is required, that we got routing
        IndexMetaData indexMetaData = clusterState.metaData().index(shardRequest.shardId.getIndex());
        MappingMetaData mappingMd = indexMetaData.mappingOrDefault(request.type());
        if (mappingMd != null && mappingMd.routing().required()) {
            if (request.routing() == null) {
                throw new RoutingMissingException(shardRequest.shardId.getIndex(), request.type(), request.id());
            }
        }

        IndexService indexService = indicesService.indexServiceSafe(shardRequest.shardId.getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRequest.shardId.id());
        SourceToParse sourceToParse = SourceToParse.source(SourceToParse.Origin.PRIMARY, request.source()).type(request.type()).id(request.id())
                .routing(request.routing()).parent(request.parent()).timestamp(request.timestamp()).ttl(request.ttl());
        long version;
        boolean created;

        if (request.opType() == IndexRequest.OpType.INDEX) {
            Engine.Index index = indexShard.prepareIndex(sourceToParse, request.version(), request.versionType(), Engine.Operation.Origin.PRIMARY, request.canHaveDuplicates());
            Mapping update = index.parsedDoc().dynamicMappingsUpdate();
            if (update != null) {
                final String indexName = indexService.index().name();
                if (indexName.equals(RiverIndexName.Conf.indexName(settings))) {
                    // With rivers, we have a chicken and egg problem if indexing
                    // the _meta document triggers a mapping update. Because we would
                    // like to validate the mapping update first, but on the other
                    // hand putting the mapping would start the river, which expects
                    // to find a _meta document
                    // So we have no choice but to index first and send mappings afterwards
                    MapperService mapperService = indexService.mapperService();
                    mapperService.merge(request.type(), new CompressedString(update.toBytes()), true);
                    indexShard.index(index);
                    mappingUpdatedAction.updateMappingOnMasterAsynchronously(indexName, request.type(), update);
                } else {
                    mappingUpdatedAction.updateMappingOnMasterSynchronously(indexName, request.type(), update);
                    indexShard.index(index);
                }
            } else {
                indexShard.index(index);
            }
            version = index.version();
            created = index.created();
        } else {
            Engine.Create create = indexShard.prepareCreate(sourceToParse,
                    request.version(), request.versionType(), Engine.Operation.Origin.PRIMARY, request.canHaveDuplicates(), request.autoGeneratedId());
            Mapping update = create.parsedDoc().dynamicMappingsUpdate();
            if (update != null) {
                final String indexName = indexService.index().name();
                if (indexName.equals(RiverIndexName.Conf.indexName(settings))) {
                    // With rivers, we have a chicken and egg problem if indexing
                    // the _meta document triggers a mapping update. Because we would
                    // like to validate the mapping update first, but on the other
                    // hand putting the mapping would start the river, which expects
                    // to find a _meta document
                    // So we have no choice but to index first and send mappings afterwards
                    MapperService mapperService = indexService.mapperService();
                    mapperService.merge(request.type(), new CompressedString(update.toBytes()), true);
                    indexShard.create(create);
                    mappingUpdatedAction.updateMappingOnMasterAsynchronously(indexName, request.type(), update);
                } else {
                    mappingUpdatedAction.updateMappingOnMasterSynchronously(indexName, request.type(), update);
                    indexShard.create(create);
                }
            } else {
                indexShard.create(create);
            }
            version = create.version();
            created = true;
        }
        if (request.refresh()) {
            try {
                indexShard.refresh("refresh_flag_index");
            } catch (Throwable e) {
                // ignore
            }
        }

        // update the version on the request, so it will be used for the replicas
        request.version(version);
        request.versionType(request.versionType().versionTypeForReplicationAndRecovery());

        assert request.versionType().validateVersionForWrites(request.version());
        return new Tuple<>(new IndexResponse(shardRequest.shardId.getIndex(), request.type(), request.id(), version, created), shardRequest.request);
    }

    private <T extends IndexingOperation> T execute(final Callable<T> callable) throws Exception {
        final ClusterStateObserver observer = new ClusterStateObserver(clusterService, null, logger);
        T index = callable.call();
        while (index.parsedDoc().dynamicMappingsUpdate() != null) {
            // Index operations on replicas can only trigger dynamic mapping
            // updates if cluster state replication is lagging compared to the
            // primary, so let's wait until no dynamic mapping updates are
            // triggered
            final AtomicReference<T> indexRef = new AtomicReference<>();
            final AtomicReference<Exception> errorRef = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);
            observer.waitForNextChange(new ClusterStateObserver.Listener() {

                @Override
                public void onTimeout(TimeValue timeout) {
                    try {
                        errorRef.set(new ElasticsearchIllegalStateException("Cannot happen since there is not timeout"));
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onNewClusterState(ClusterState state) {
                    try {
                        indexRef.set(callable.call());
                    } catch (Exception e) {
                        errorRef.set(e);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onClusterServiceClose() {
                    try {
                        errorRef.set(new ElasticsearchIllegalStateException("Shard closing..."));
                    } finally {
                        latch.countDown();
                    }
                }

            });
            latch.await();
            if (errorRef.get() != null) {
                throw errorRef.get();
            }
            index = indexRef.get();
            assert index != null;
        }
        return index;
    }

    @Override
    protected void shardOperationOnReplica(ReplicaOperationRequest shardRequest) throws Exception {
        IndexService indexService = indicesService.indexServiceSafe(shardRequest.shardId.getIndex());
        final IndexShard indexShard = indexService.shardSafe(shardRequest.shardId.id());
        final IndexRequest request = shardRequest.request;
        final SourceToParse sourceToParse = SourceToParse.source(SourceToParse.Origin.REPLICA, request.source()).type(request.type()).id(request.id())
                .routing(request.routing()).parent(request.parent()).timestamp(request.timestamp()).ttl(request.ttl());
        if (request.opType() == IndexRequest.OpType.INDEX) {
            Engine.Index index = execute(new Callable<Engine.Index>() {
                @Override
                public Engine.Index call() throws Exception {
                    return indexShard.prepareIndex(sourceToParse, request.version(), request.versionType(), Engine.Operation.Origin.REPLICA, request.canHaveDuplicates());
                }
            });
            indexShard.index(index);
        } else {
            Engine.Create create = execute(new Callable<Engine.Create>() {
                @Override
                public Engine.Create call() throws Exception {
                    return indexShard.prepareCreate(sourceToParse, request.version(), request.versionType(), Engine.Operation.Origin.REPLICA, request.canHaveDuplicates(), request.autoGeneratedId());
                }
            });
            indexShard.create(create);
        }
        if (request.refresh()) {
            try {
                indexShard.refresh("refresh_flag_index");
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
