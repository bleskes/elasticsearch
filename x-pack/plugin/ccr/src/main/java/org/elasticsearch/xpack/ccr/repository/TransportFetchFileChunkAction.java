/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr.repository;

import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.nodes.BaseNodeRequest;
import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.action.support.nodes.BaseNodesRequest;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.action.support.nodes.TransportNodesAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportFetchFileChunkAction extends
    TransportNodesAction<TransportFetchFileChunkAction.Request, TransportFetchFileChunkAction.Response,
        TransportFetchFileChunkAction.ChunkRequest, TransportFetchFileChunkAction.ChunkResponse
        > {

    public final static Action<Response> ACTION = new Action<Response>("cluster:admin/xpack/ccr/restore/file_chunk") {
        @Override
        public Response newResponse() {
            return new Response();
        }
    };

    private final RemoteClusterRestoreSourceService restoreSourceService;

    @Inject
    public TransportFetchFileChunkAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                         TransportService transportService, ActionFilters actionFilters,
                                         RemoteClusterRestoreSourceService restoreSourceService
    ) {
        super(settings, ACTION.name(), threadPool, clusterService, transportService, actionFilters, Request::new, ChunkRequest::new,
            ThreadPool.Names.GENERIC, ChunkResponse.class);
        this.restoreSourceService = restoreSourceService;
    }

    @Override
    protected Response newResponse(Request request, List<ChunkResponse> chunkResponses, List<FailedNodeException> failures) {
        return new Response(clusterService.getClusterName(), chunkResponses, failures);
    }

    @Override
    protected ChunkRequest newNodeRequest(String nodeId, Request request) {
        return request.chunkRequest;
    }

    @Override
    protected ChunkResponse newNodeResponse() {
        return new ChunkResponse();
    }

    @Override
    protected ChunkResponse nodeOperation(ChunkRequest request) {
        Engine.IndexCommitRef snapshot = restoreSourceService.getCommit(request.sessionUUID);
        try (IndexInput in = snapshot.getIndexCommit().getDirectory().openInput(request.fileName, IOContext.READ)) {
            byte[] chunk = new byte[request.size];
            in.readBytes(chunk, 0, request.size);
            return new ChunkResponse(chunk);
        } catch (IOException e) {
            throw new ElasticsearchException(e);
        }
    }

    public static byte[] readBytesFromFile(Client client, String nodeId, String sessionUUID, String filename, long offset, int size) {
        Request request = new Request(new ChunkRequest(sessionUUID, filename, offset, size));
        request.nodesIds(nodeId);
        Response response = client.execute(ACTION, request).actionGet();
        return response.getChunk().chunk;
    }


    public static class ChunkRequest extends BaseNodeRequest {
        private String sessionUUID;
        private String fileName;
        private long offset;
        private int size;


        ChunkRequest() {

        }

        ChunkRequest(String sessionUUID, String fileName, long offset, int size) {
            this.sessionUUID = sessionUUID;
            this.fileName = fileName;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            sessionUUID = in.readString();
            fileName = in.readString();
            offset = in.readVLong();
            size = in.readVInt();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(sessionUUID);
            out.writeString(fileName);
            out.writeVLong(offset);
            out.writeVInt(size);
        }
    }

    public static class ChunkResponse extends BaseNodeResponse {

        private byte[] chunk;

        private ChunkResponse() {

        }

        private ChunkResponse(byte[] chunk) {
            this.chunk = chunk;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeByteArray(chunk);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            chunk = in.readByteArray();
        }

        public static ChunkResponse newFromStream(StreamInput in) throws IOException {
            ChunkResponse response = new ChunkResponse();
            response.readFrom(in);
            return response;
        }
    }

    public static class Request extends BaseNodesRequest<Request> {
        private ChunkRequest chunkRequest;

        Request() {
        }

        Request(ChunkRequest chunkRequest) {
            this.chunkRequest = chunkRequest;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            chunkRequest.writeTo(out);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            chunkRequest = new ChunkRequest();
            chunkRequest.readFrom(in);
        }
    }

    public static class Response extends BaseNodesResponse<ChunkResponse> {

        Response() {
        }

        public Response(ClusterName clusterName, List<ChunkResponse> chunkResponses, List<FailedNodeException> failures) {
            super(clusterName, chunkResponses, failures);
        }

        ChunkResponse getChunk() {
            if (hasFailures()) {
                throw failures().get(0);
            } else {
                return getNodes().get(0);
            }
        }

        @Override
        protected List<ChunkResponse> readNodesFrom(StreamInput in) throws IOException {
            return in.readList(ChunkResponse::newFromStream);
        }

        @Override
        protected void writeNodesTo(StreamOutput out, List<ChunkResponse> nodes) throws IOException {
            out.writeList(nodes);
        }
    }
}
