/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.bulk.BulkRequest;
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
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.messages.Messages;

import java.io.IOException;
import java.util.Objects;

public class PutJobAction extends Action<PutJobAction.Request, PutJobAction.Response, PutJobAction.RequestBuilder> {

    private static final DeprecationLogger DEPRECATION_LOGGER =
            new DeprecationLogger(Loggers.getLogger(PutJobAction.class));

    public static final PutJobAction INSTANCE = new PutJobAction();
    public static final String NAME = "cluster:admin/xpack/ml/job/put";

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

        public static Request parseRequest(String jobId, XContentParser parser) {
            Job.Builder job = Job.PARSER.apply(parser, null);
            if (job.getId() == null) {
                job.setId(jobId);
            } else if (!Strings.isNullOrEmpty(jobId) && !jobId.equals(job.getId())) {
                // If we have both URI and body job ID, they must be identical
                throw new IllegalArgumentException(Messages.getMessage(Messages.INCONSISTENT_ID, Job.ID.getPreferredName(),
                        job.getId(), jobId));
            }

            return new Request(job);
        }

        private Job.Builder job;

        public Request(Job.Builder job) {
            this.job = job;
        }

        Request() {
        }

        public Job.Builder getJob() {
            return job;
        }

        @Override
        public ActionRequestValidationException validate() {
            if (job != null && job.getDataDescription() != null &&
                    job.getDataDescription().getFormat() == DataDescription.DataFormat.DELIMITED) {
                DEPRECATION_LOGGER.deprecated("Creating jobs with delimited data format is " +
                        "deprecated. Please use JSON instead.");
            }
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            job = new Job.Builder(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            job.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            job.toXContent(builder, params);
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Objects.equals(job, request.job);
        }

        @Override
        public int hashCode() {
            return Objects.hash(job);
        }

        @Override
        public final String toString() {
            return Strings.toString(this);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PutJobAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse implements ToXContentObject {

        private Job job;

        public Response(boolean acked, Job job) {
            super(acked);
            this.job = job;
        }

        Response() {
        }

        public Job getResponse() {
            return job;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            readAcknowledged(in);
            job = new Job(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            writeAcknowledged(out);
            job.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            // Don't serialize acknowledged because current api directly serializes the job details
            builder.startObject();
            job.doXContentBody(builder, params);
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return Objects.equals(job, response.job);
        }

        @Override
        public int hashCode() {
            return Objects.hash(job);
        }
    }

    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final JobManager jobManager;
        private final XPackLicenseState licenseState;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                ThreadPool threadPool, XPackLicenseState licenseState, ActionFilters actionFilters,
                IndexNameExpressionResolver indexNameExpressionResolver, JobManager jobManager) {
            super(settings, PutJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.licenseState = licenseState;
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
            jobManager.putJob(request, state, listener);
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }

        @Override
        protected void doExecute(Task task, Request request, ActionListener<Response> listener) {
            if (licenseState.isMachineLearningAllowed()) {
                super.doExecute(task, request, listener);
            } else {
                listener.onFailure(LicenseUtils.newComplianceException(XPackPlugin.MACHINE_LEARNING));
            }
        }
    }
}
