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
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;

import java.io.IOException;
import java.util.Objects;

public class PutJobAction extends Action<PutJobAction.Request, PutJobAction.Response, PutJobAction.RequestBuilder> {

    public static final PutJobAction INSTANCE = new PutJobAction();
    public static final String NAME = "cluster:admin/prelert/job/put";

    private PutJobAction() {
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

    public static class Request extends AcknowledgedRequest<Request> implements ToXContent {

        public static Request parseRequest(XContentParser parser, ParseFieldMatcherSupplier matcherSupplier) {
            JobConfiguration jobConfiguration = JobConfiguration.PARSER.apply(parser, matcherSupplier);
            return new Request(jobConfiguration);
        }

        private JobConfiguration jobConfiguration;
        private boolean overwrite;

        public Request(JobConfiguration jobConfiguration) {
            this.jobConfiguration = jobConfiguration;
        }

        Request() {
        }

        public JobConfiguration getJobConfiguration() {
            return jobConfiguration;
        }

        public boolean isOverwrite() {
            return overwrite;
        }

        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobConfiguration = new JobConfiguration(in);
            overwrite = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            jobConfiguration.writeTo(out);
            out.writeBoolean(overwrite);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            jobConfiguration.toXContent(builder, params);
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return overwrite == request.overwrite &&
                    Objects.equals(jobConfiguration, request.jobConfiguration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobConfiguration, overwrite);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PutJobAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse implements ToXContent {

        private JobDetails jobDetails;

        public Response(JobDetails jobDetails) {
            super(true);
            this.jobDetails = jobDetails;
        }

        Response() {
        }

        public JobDetails getResponse() {
            return jobDetails;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobDetails = new JobDetails(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            jobDetails.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            // Don't serialize acknowledged because current api directly serializes the job details
            jobDetails.doXContentBody(builder, params);
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return Objects.equals(jobDetails, response.jobDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobDetails);
        }
    }

    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final JobManager jobManager;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                               ThreadPool threadPool, ActionFilters actionFilters,
                               IndexNameExpressionResolver indexNameExpressionResolver, JobManager jobManager) {
            super(settings, PutJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.jobManager = jobManager;
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
            jobManager.putJob(request, listener);
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }
    }
}
