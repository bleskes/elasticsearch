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
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
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
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.Optional;

public class GetJobAction extends Action<GetJobAction.Request, GetJobAction.Response, GetJobAction.RequestBuilder> {

    public static final GetJobAction INSTANCE = new GetJobAction();
    public static final String NAME = "cluster:admin/prelert/job/get";

    private GetJobAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends MasterNodeReadRequest<Request> {

        private String jobId;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
        }
    }

    public static class RequestBuilder extends MasterNodeReadOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetJobAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        private SingleDocument result;

        public Response(SingleDocument job) {
            this.result = job;
        }

        public Response() {}

        public SingleDocument getResult() {
            return result;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            result = new SingleDocument(in.readString(), in.readBytesReference());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(result.getType());
            out.writeBytesReference(result.getDocument());
        }

        @Override
        public RestStatus status() {
            return result.status();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return result.toXContent(builder, params);
        }
    }


    public static class TransportAction extends TransportMasterNodeReadAction<Request, Response> {

        private final PrelertServices prelertServices;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                     ThreadPool threadPool, ActionFilters actionFilters,
                                     IndexNameExpressionResolver indexNameExpressionResolver, PrelertServices prelertServices) {
            super(settings, GetJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.prelertServices = prelertServices;
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
            logger.debug("Get job '" + request.getJobId() + "'");
            Optional<JobDetails> optionalJob = prelertServices.getJobManager().getJob(request.getJobId());
            SingleDocument jobDocument = optionalJob.isPresent() ? createJobDocument(optionalJob.get()) : SingleDocument.empty(JobDetails.TYPE);
            if (jobDocument.isExists()) {
                logger.debug("Returning job '" + optionalJob.get().getJobId() + "'");
            }
            else {
                logger.debug(String.format("Cannot find job '%s'", request.getJobId()));
            }
            listener.onResponse(new Response(jobDocument));
        }

        private SingleDocument createJobDocument(JobDetails job) throws JsonProcessingException {
            byte[] asBytes = objectMapper.writeValueAsBytes(job);
            return new SingleDocument(JobDetails.TYPE, new BytesArray(asBytes));
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
        }
    }

}
