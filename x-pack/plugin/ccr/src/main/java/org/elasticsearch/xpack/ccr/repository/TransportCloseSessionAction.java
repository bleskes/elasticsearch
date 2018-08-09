/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr.repository;

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
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportCloseSessionAction extends
    TransportNodesAction<TransportCloseSessionAction.Request, TransportCloseSessionAction.Response,
        TransportCloseSessionAction.SessionRequest, TransportCloseSessionAction.SessionResponse
        > {

    public final static Action<Response> ACTION = new Action<Response>("cluster:admin/xpack/ccr/restore/close_session") {
        @Override
        public Response newResponse() {
            return new Response();
        }
    };

    private final RemoteClusterRestoreSourceService restoreSourceService;

    @Inject
    public TransportCloseSessionAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                       TransportService transportService, ActionFilters actionFilters,
                                       RemoteClusterRestoreSourceService restoreSourceService
    ) {
        super(settings, ACTION.name(), threadPool, clusterService, transportService, actionFilters, Request::new, SessionRequest::new,
            ThreadPool.Names.GENERIC, SessionResponse.class);
        this.restoreSourceService = restoreSourceService;
    }

    @Override
    protected Response newResponse(Request request, List<SessionResponse> chunkResponses, List<FailedNodeException> failures) {
        return new Response(clusterService.getClusterName(), chunkResponses, failures);
    }

    @Override
    protected SessionRequest newNodeRequest(String nodeId, Request request) {
        return request.sessionRequest;
    }

    @Override
    protected SessionResponse newNodeResponse() {
        return new SessionResponse();
    }

    @Override
    protected SessionResponse nodeOperation(SessionRequest request) {
        restoreSourceService.closeCommit(request.sessionUUID);
        return new SessionResponse(clusterService.localNode());
    }

    public static void closeSession(Client client, TransportCreateRestoreSessionAction.Session session) {
        Request request = new Request(new SessionRequest(session.getNodeId(), session.getSessionUUID()));
        request.nodesIds(session.getNodeId());
        Response response = client.execute(ACTION, request).actionGet();
        response.throwIfFailed();
    }


    public static class SessionRequest extends BaseNodeRequest {
        private String sessionUUID;

        SessionRequest() {

        }

        SessionRequest(String nodeId, String sessionUUID) {
            super(nodeId);
            this.sessionUUID = sessionUUID;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            sessionUUID = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(sessionUUID);
        }
    }

    public static class SessionResponse extends BaseNodeResponse {

        private SessionResponse() {

        }

        private SessionResponse(DiscoveryNode node) {
            super(node);
        }

        public static SessionResponse newFromStream(StreamInput in) throws IOException {
            SessionResponse response = new SessionResponse();
            response.readFrom(in);
            return response;
        }
    }

    public static class Request extends BaseNodesRequest<Request> {
        private SessionRequest sessionRequest;

        Request() {
        }

        Request(SessionRequest sessionRequest) {
            this.sessionRequest = sessionRequest;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            sessionRequest.writeTo(out);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            sessionRequest = new SessionRequest();
            sessionRequest.readFrom(in);
        }
    }

    public static class Response extends BaseNodesResponse<SessionResponse> {

        Response() {
        }

        public Response(ClusterName clusterName, List<SessionResponse> chunkResponses, List<FailedNodeException> failures) {
            super(clusterName, chunkResponses, failures);
        }

        void throwIfFailed() {
            if (hasFailures()) {
                throw failures().get(0);
            }
        }

        @Override
        protected List<SessionResponse> readNodesFrom(StreamInput in) throws IOException {
            return in.readList(SessionResponse::newFromStream);
        }

        @Override
        protected void writeNodesTo(StreamOutput out, List<SessionResponse> nodes) throws IOException {
            out.writeList(nodes);
        }
    }
}
