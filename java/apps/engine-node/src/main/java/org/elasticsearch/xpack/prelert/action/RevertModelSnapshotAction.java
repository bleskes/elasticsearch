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
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
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
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchBulkDeleterFactory;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.OldDataRemover;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.elasticsearch.action.ValidateActions.addValidationError;

public class RevertModelSnapshotAction extends Action<RevertModelSnapshotAction.Request, RevertModelSnapshotAction.Response, RevertModelSnapshotAction.RequestBuilder> {

    public static final RevertModelSnapshotAction INSTANCE = new RevertModelSnapshotAction();
    public static final String NAME = "indices:admin/prelert/modelsnapshots/revert";

    private RevertModelSnapshotAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends AcknowledgedRequest<Request> {

        private String jobId;
        private String time;
        private String snapshotId;
        private String description;
        private boolean deleteInterveningResults;

        private Request() {
        }

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
        }

        public String getJobId() {
            return jobId;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getSnapshotId() {
            return snapshotId;
        }

        public void setSnapshotId(String snapshotId) {
            this.snapshotId = snapshotId;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean getDeleteInterveningResults() {
            return deleteInterveningResults;
        }

        public void setDeleteInterveningResults(boolean deleteInterveningResults) {
            this.deleteInterveningResults = deleteInterveningResults;
        }

        @Override
        public ActionRequestValidationException validate() {
            ActionRequestValidationException validationException = null;
            if (time == null && snapshotId == null && description == null) {
                validationException = addValidationError(Messages.getMessage(Messages.REST_INVALID_REVERT_PARAMS), validationException);
            }
            return validationException;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            time = in.readOptionalString();
            snapshotId = in.readOptionalString();
            description = in.readOptionalString();
            deleteInterveningResults = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeOptionalString(time);
            out.writeOptionalString(snapshotId);
            out.writeOptionalString(description);
            out.writeBoolean(deleteInterveningResults);
        }
    }

    static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client) {
            super(client, INSTANCE, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse implements StatusToXContent {

        private SingleDocument response;

        public Response() {
            super(false);
            response = SingleDocument.empty(ModelSnapshot.TYPE.getPreferredName());
        }

        public Response(ModelSnapshot modelSnapshot, ObjectMapper objectMapper) throws JsonProcessingException {
            super(true);
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

    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final JobManager jobManager;
        private final JobProvider jobProvider;
        private final ElasticsearchBulkDeleterFactory bulkDeleterFactory;

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                JobManager jobManager, ElasticsearchJobProvider jobProvider, ClusterService clusterService,
                ElasticsearchBulkDeleterFactory bulkDeleterFactory) {
            super(settings, NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.jobManager = jobManager;
            this.jobProvider = jobProvider;
            this.bulkDeleterFactory = bulkDeleterFactory;
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        protected Response newResponse() {
            return new Response();
        }

        @Override
        protected void masterOperation(Request request, ClusterState state, ActionListener<Response> listener) throws Exception {
            logger.debug("Received request to revert to time '" + request.getTime() +
                    "' description '" + request.getDescription() + "' snapshot id '" +
                    request.getSnapshotId() + "' for job '" + request.getJobId() + "', deleting intervening " +
                    " results: " + request.getDeleteInterveningResults());

            if (request.getTime() == null && request.getSnapshotId() == null && request.getDescription() == null) {
                throw new IllegalStateException(Messages.getMessage(Messages.REST_INVALID_REVERT_PARAMS));
            }

            Optional<JobDetails> job = jobManager.getJob(request.getJobId(), clusterService.state());
            if (job.isPresent() && job.get().getStatus().equals(JobStatus.RUNNING)) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.REST_JOB_NOT_CLOSED_REVERT),
                        ErrorCodes.JOB_NOT_CLOSED);
            }

            ModelSnapshot modelSnapshot = getModelSnapshot(request, jobProvider);
            if (request.getDeleteInterveningResults()) {
                listener = wrapListener(listener, modelSnapshot, request.getJobId());
            }
            jobManager.revertSnapshot(request, listener, modelSnapshot);
        }

        private ModelSnapshot getModelSnapshot(Request request, JobProvider provider) {
            logger.info("Reverting to snapshot '" + request.getSnapshotId() +
                    "' for time '" + request.getTime() + "'");

            List<ModelSnapshot> revertCandidates;
            try {
                revertCandidates = provider.modelSnapshots(request.getJobId(), 0, 1,
                        null, request.getTime(), ModelSnapshot.TIMESTAMP.getPreferredName(), true,
                        request.getSnapshotId(), request.getDescription()).hits();
            } catch (UnknownJobException e) {
                throw ExceptionsHelper.missingException(request.getJobId());
            }

            if (revertCandidates == null || revertCandidates.isEmpty()) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.REST_NO_SUCH_MODEL_SNAPSHOT, request.getJobId()), ErrorCodes.NO_SUCH_MODEL_SNAPSHOT);
            }
            ModelSnapshot modelSnapshot = revertCandidates.get(0);

            // The quantiles can be large, and totally dominate the output - it's
            // clearer to remove them
            modelSnapshot.setQuantiles(null);
            return modelSnapshot;
        }

        private ActionListener<RevertModelSnapshotAction.Response> wrapListener(
                ActionListener<RevertModelSnapshotAction.Response> listener, ModelSnapshot modelSnapshot, String jobId) {

            // If we need to delete buckets that occurred after the snapshot, we wrap
            // the listener with one that invokes the OldDataRemover on acknowledged responses
            return ActionListener.wrap(response -> {
                if (response.isAcknowledged()) {
                    Date deleteAfter = modelSnapshot.getLatestRecordTimeStamp();
                    logger.debug("Removing intervening records: last record: " + deleteAfter +
                            ", last result: " + modelSnapshot.getLatestResultTimeStamp());

                    logger.info("Deleting buckets after '" + deleteAfter + "'");

                    // NORELEASE: OldDataRemover is basically delete-by-query. We should replace this
                    // whole abstraction with DBQ eventually
                    OldDataRemover remover = new OldDataRemover(jobProvider, bulkDeleterFactory);
                    remover.deleteResultsAfter(jobId, deleteAfter.getTime() + 1);
                }
                listener.onResponse(response);
            }, listener::onFailure);
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }
    }

}
