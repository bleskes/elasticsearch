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

package org.elasticsearch.xpack.prelert.action;

import java.io.IOException;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.config.verification.DetectorVerifier;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidateDetectorAction
extends Action<ValidateDetectorAction.Request, ValidateDetectorAction.Response, ValidateDetectorAction.RequestBuilder> {

    public final static ValidateDetectorAction INSTANCE = new ValidateDetectorAction();
    public final static String NAME = "cluster:admin/prelert/validate/detector";

    protected ValidateDetectorAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, INSTANCE);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        protected RequestBuilder(ElasticsearchClient client, ValidateDetectorAction action) {
            super(client, action, new Request());
        }

    }

    public static class Request extends ActionRequest<Request> {

        private BytesReference detector;

        Request() {
            this.detector = null;
        }

        public Request(BytesReference detector) {
            this.detector = detector;
        }

        public BytesReference getDetector() {
            return detector;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBytesReference(detector);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            detector = in.readBytesReference();
        }

    }

    public static class Response extends AcknowledgedResponse {

        public Response() {
            super();
        }

        public Response(boolean acknowledged) {
            super(acknowledged);
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService, ThreadPool threadPool,
                ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
            super(settings, ValidateDetectorAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                    Request::new);
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            ObjectMapper objectMapper = new ObjectMapper();
            Detector detector;
            try {
                detector = objectMapper.readValue(request.getDetector().toBytesRef().bytes, Detector.class);
            } catch (IOException e) {
                throw new ParsingException(-1, -1, "Failed to parse detector", e);
            }
            DetectorVerifier.verify(detector, false);
            listener.onResponse(new Response(true));
        }

    }
}
