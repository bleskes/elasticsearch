/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr.repository;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.single.shard.SingleShardRequest;
import org.elasticsearch.action.support.single.shard.TransportSingleShardAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardNotFoundException;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

public class TransportCreateRestoreSessionAction extends
    TransportSingleShardAction<TransportCreateRestoreSessionAction.Request, TransportCreateRestoreSessionAction.Session> {

    public final static Action<Session> ACTION = new Action<Session>("cluster:admin/xpack/ccr/restore/create_session") {
        @Override
        public Session newResponse() {
            return new Session();
        }
    };

    private final IndicesService indicesService;
    private final RemoteClusterRestoreSourceService restoreSourceService;

    @Inject
    public TransportCreateRestoreSessionAction(Settings settings, ThreadPool threadPool,
                                               ClusterService clusterService, TransportService transportService,
                                               IndicesService indicesService, RemoteClusterRestoreSourceService restoreSourceService,
                                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, ACTION.name(), threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
            Request::new, ThreadPool.Names.GENERIC);
        this.indicesService = indicesService;
        this.restoreSourceService = restoreSourceService;
    }

    public static Session createSession(Client client, ShardId shardId) {
        return client.execute(ACTION, new Request(shardId)).actionGet();
    }

    @Override
    protected Session shardOperation(Request request, ShardId shardId) throws IOException {
        IndexShard indexShard = indicesService.getShardOrNull(shardId);
        if (indexShard == null) {
            throw new ShardNotFoundException(shardId);
        }
        Engine.IndexCommitRef commit = indexShard.acquireSafeIndexCommit();
        final String uuid = UUIDs.randomBase64UUID();
        restoreSourceService.addCommit(uuid, commit);
        final Store.MetadataSnapshot snapshot;
        indexShard.store().incRef();
        try {
            snapshot = indexShard.store().getMetadata(commit.getIndexCommit());
        } finally {
            indexShard.store().decRef();
        }
        return new Session(snapshot, indexShard.routingEntry().currentNodeId(), uuid);
    }

    @Override
    protected Session newResponse() {
        return new Session();
    }

    @Override
    protected boolean resolveIndex(Request request) {
        return false;
    }

    @Override
    protected ShardsIterator shards(ClusterState state, InternalRequest request) {
        final ShardId shardId = request.request().shardId;
        // sadly the index uuid is not correct if we restore with a rename (Which we do)
        IndexShardRoutingTable shardRoutingTable = state.routingTable().shardRoutingTable(shardId.getIndexName(), shardId.id());
        return shardRoutingTable.primaryShardIt();
    }

    public static class Request extends SingleShardRequest<Request> {
        private ShardId shardId;

        Request() {

        }

        public Request(ShardId shardId) {
            this.index = shardId.getIndexName();
            this.shardId = shardId;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            shardId = ShardId.readShardId(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            shardId.writeTo(out);
        }
    }

    public static class Session extends ActionResponse {
        private Store.MetadataSnapshot metaData;
        private String nodeId;
        private String sessionUUID;

        Session() {

        }

        Session(Store.MetadataSnapshot metaData, String nodeId, String sessionUUID) {
            this.metaData = metaData;
            this.nodeId = nodeId;
            this.sessionUUID = sessionUUID;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            metaData.writeTo(out);
            out.writeString(nodeId);
            out.writeString(sessionUUID);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            metaData = new Store.MetadataSnapshot(in);
            nodeId = in.readString();
            sessionUUID = in.readString();
        }

        public Store.MetadataSnapshot getMetadata() {
            return metaData;
        }

        public String getSessionUUID() {
            return sessionUUID;
        }

        public String getNodeId() {
            return nodeId;
        }
    }
}
