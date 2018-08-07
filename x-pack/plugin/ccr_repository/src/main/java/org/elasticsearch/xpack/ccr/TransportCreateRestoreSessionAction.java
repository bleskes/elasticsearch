/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.single.shard.SingleShardRequest;
import org.elasticsearch.action.support.single.shard.TransportSingleShardAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.UUIDs;
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
import java.util.function.Supplier;

public class TransportCreateRestoreSessionAction extends
    TransportSingleShardAction<TransportCreateRestoreSessionAction.Request, TransportCreateRestoreSessionAction.Response> {

    public final static Action<Response> ACTION = new Action<Response>("cluster:admin/xpack/ccr/restore/create_session") {
        @Override
        public Response newResponse() {
            return new Response();
        }
    };

    private final IndicesService indicesService;
    private final RemoteClusterRestoreSourceService restoreSourceService;

    protected TransportCreateRestoreSessionAction(Settings settings, ThreadPool threadPool,
                                                  ClusterService clusterService, TransportService transportService,
                                                  IndicesService indicesService, RemoteClusterRestoreSourceService restoreSourceService,
                                                  ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                                  Supplier<Request> request, String executor) {
        super(settings, ACTION.name(), threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver, request,
            executor);
        this.indicesService = indicesService;
        this.restoreSourceService = restoreSourceService;
    }

    @Override
    protected Response shardOperation(Request request, ShardId shardId) throws IOException {
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
        return new Response(snapshot, indexShard.routingEntry().currentNodeId(), uuid);
    }

    @Override
    protected Response newResponse() {
        return null;
    }

    @Override
    protected boolean resolveIndex(Request request) {
        return false;
    }

    @Override
    protected ShardsIterator shards(ClusterState state, InternalRequest request) {
        IndexShardRoutingTable shardRoutingTable =
            state.routingTable().shardRoutingTable(request.concreteIndex(), request.request().shardId);
        return shardRoutingTable.primaryShardIt();
    }

    public static class Request extends SingleShardRequest<Request> {
        private int shardId;

        Request() {

        }

        public Request(String index, int shardId) {
            this.index = index;
            this.shardId = shardId;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }
    }

    public static class Response extends ActionResponse {
        private Store.MetadataSnapshot snapshot;
        private String nodeId;
        private String sessionUUID;

        Response() {

        }

        Response(Store.MetadataSnapshot snapshot, String nodeId, String sessionUUID) {
            this.snapshot = snapshot;
            this.nodeId = nodeId;
            this.sessionUUID = sessionUUID;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            snapshot.writeTo(out);
            out.writeString(nodeId);
            out.writeString(sessionUUID);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            snapshot = new Store.MetadataSnapshot(in);
            nodeId = in.readString();
            sessionUUID = in.readString();
        }
    }
}
