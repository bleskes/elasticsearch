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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.List;

public class PutModelSnapshotDescriptionAction extends Action<PutModelSnapshotDescriptionAction.Request, PutModelSnapshotDescriptionAction.Response, PutModelSnapshotDescriptionAction.RequestBuilder> {

    public static final PutModelSnapshotDescriptionAction INSTANCE = new PutModelSnapshotDescriptionAction();
    public static final String NAME = "cluster:admin/prelert/modelsnapshot/put/description";

    private PutModelSnapshotDescriptionAction() {
        super(NAME);
    }

    @Override
    public PutModelSnapshotDescriptionAction.RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public PutModelSnapshotDescriptionAction.Response newResponse() {
        return new Response();
    }

    public static class Request extends ActionRequest<Request> {

        private String jobId;
        private String snapshotId;
        private String description;

        private Request() {
        }

        public Request(String jobId, String snapshotId, String description) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
            this.snapshotId = ExceptionsHelper.requireNonNull(snapshotId, "snapshotId");
            this.description = ExceptionsHelper.requireNonNull(description, "description");
        }

        public String getJobId() {
            return jobId;
        }

        public String getSnapshotId() {
            return snapshotId;
        }

        public String getDescriptionString() {
            return description;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            snapshotId = in.readString();
            description = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeString(snapshotId);
            out.writeString(description);
        }
    }


    public static class Response extends ActionResponse implements StatusToXContent {

        private SingleDocument response;

        public Response() {
            response = SingleDocument.empty(ModelSnapshot.TYPE.getPreferredName());
        }

        public Response(ModelSnapshot modelSnapshot, ObjectMapper objectMapper) throws JsonProcessingException {
            byte[] asBytes = objectMapper.writeValueAsBytes(modelSnapshot);
            response = new SingleDocument(ModelSnapshot.TYPE.getPreferredName(), new BytesArray(asBytes));
        }

        public SingleDocument getResponse() {
            return response;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            response = new SingleDocument(in.readString(), in.readBytesReference());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(response.getType());
            out.writeBytesReference(response.getDocumentBytes());
        }

        @Override
        public RestStatus status() {
            return response.status();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return response.toXContent(builder, params);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PutModelSnapshotDescriptionAction action) {
            super(client, action, new Request());
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final ElasticsearchJobProvider jobProvider;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               ElasticsearchJobProvider jobProvider) {
            super(settings, NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, Request::new);
            this.jobProvider = jobProvider;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {

            logger.debug("Received request to change model snapshot description using '"
                    + request.getDescriptionString() + "' for snapshot ID '" + request.getSnapshotId() +
                    "' for job '" + request.getJobId() + "'");

            List<ModelSnapshot> changeCandidates = getChangeCandidates(request);
            checkForClashes(request);

            if (changeCandidates.size() > 1) {
                logger.warn("More than one model found for [jobId: " + request.getJobId()
                        + ", snapshotId: " + request.getSnapshotId() + "] tuple.");
            }
            ModelSnapshot modelSnapshot = changeCandidates.get(0);
            modelSnapshot.setDescription(request.getDescriptionString());
            try {
                jobProvider.updateModelSnapshot(request.getJobId(), modelSnapshot, false);
            } catch (UnknownJobException e) {
                throw ExceptionsHelper.missingException(request.getJobId());
            }

            modelSnapshot.setDescription(request.getDescriptionString());

            // The quantiles can be large, and totally dominate the output - it's
            // clearer to remove them
            modelSnapshot.setQuantiles(null);

            try {
                listener.onResponse(new Response(modelSnapshot, objectMapper));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        private List<ModelSnapshot> getChangeCandidates(Request request) {
            List<ModelSnapshot> changeCandidates = getModelSnapshots(request.getJobId(), request.getSnapshotId(), null);
            if (changeCandidates == null || changeCandidates.isEmpty()) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.REST_NO_SUCH_MODEL_SNAPSHOT, ErrorCodes.NO_SUCH_MODEL_SNAPSHOT);
            }
            return changeCandidates;
        }

        private void checkForClashes(Request request) {
            List<ModelSnapshot> clashCandidates = getModelSnapshots(request.getJobId(), null, request.getDescriptionString());
            if (clashCandidates != null && !clashCandidates.isEmpty()) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.REST_DESCRIPTION_ALREADY_USED, ErrorCodes.DESCRIPTION_ALREADY_USED);
            }
        }

        private List<ModelSnapshot> getModelSnapshots(String jobId, String snapshotId, String description) {
            return jobProvider.modelSnapshots(jobId, 0, 1, null, null, null, true, snapshotId, description).hits();
        }

    }

}
