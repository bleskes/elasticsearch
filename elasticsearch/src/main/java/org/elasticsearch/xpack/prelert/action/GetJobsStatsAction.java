/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.action;

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
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.manager.AutodetectProcessManager;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerStatus;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GetJobsStatsAction extends Action<GetJobsStatsAction.Request, GetJobsStatsAction.Response, GetJobsStatsAction.RequestBuilder> {

    public static final GetJobsStatsAction INSTANCE = new GetJobsStatsAction();
    public static final String NAME = "cluster:admin/prelert/jobs/stats/get";

    private static final String ALL = "_all";
    private static final String CONFIG = "config";
    private static final String DATA_COUNTS = "data_counts";
    private static final String MODEL_SIZE_STATS = "model_size_stats";
    private static final String SCHEDULER_STATUS = "scheduler_status";
    private static final String STATUS = "status";

    private GetJobsStatsAction() {
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

    public static class Request extends ActionRequest {

        private String jobId;

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, Job.ID.getPreferredName());
        }

        Request() {}

        public String getJobId() {
            return jobId;
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

        @Override
        public int hashCode() {
            return Objects.hash(jobId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(jobId, other.jobId);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetJobsStatsAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        public static class JobStats implements ToXContent, Writeable {
            private final String jobId;
            private DataCounts dataCounts;
            @Nullable
            private ModelSizeStats modelSizeStats;
            private JobStatus status;
            @Nullable
            private SchedulerStatus schedulerStatus;

            JobStats(String jobId, DataCounts dataCounts, @Nullable ModelSizeStats modelSizeStats, JobStatus status,
                     @Nullable SchedulerStatus schedulerStatus) {
                this.jobId = Objects.requireNonNull(jobId);
                this.dataCounts = Objects.requireNonNull(dataCounts);
                this.modelSizeStats = modelSizeStats;
                this.status = Objects.requireNonNull(status);
                this.schedulerStatus = schedulerStatus;
            }

            JobStats(StreamInput in) throws IOException {
                jobId = in.readString();
                dataCounts = new DataCounts(in);
                modelSizeStats = in.readOptionalWriteable(ModelSizeStats::new);
                status = JobStatus.fromStream(in);
                schedulerStatus = in.readOptionalWriteable(SchedulerStatus::fromStream);
            }

            public String getJobid() {
                return jobId;
            }

            public DataCounts getDataCounts() {
                return dataCounts;
            }

            public ModelSizeStats getModelSizeStats() {
                return modelSizeStats;
            }

            public SchedulerStatus getSchedulerStatus() {
                return schedulerStatus;
            }

            public JobStatus getStatus() {
                return status;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                builder.startObject();
                builder.field(Job.ID.getPreferredName(), jobId);
                builder.field(DATA_COUNTS, dataCounts);
                if (modelSizeStats != null) {
                    builder.field(MODEL_SIZE_STATS, modelSizeStats);
                }
                builder.field(STATUS, status);
                if (schedulerStatus != null) {
                    builder.field(SCHEDULER_STATUS, schedulerStatus);
                }
                builder.endObject();

                return builder;
            }

            @Override
            public void writeTo(StreamOutput out) throws IOException {
                out.writeString(jobId);
                dataCounts.writeTo(out);
                out.writeOptionalWriteable(modelSizeStats);
                status.writeTo(out);
                out.writeOptionalWriteable(schedulerStatus);
            }

            @Override
            public int hashCode() {
                return Objects.hash(jobId, dataCounts, modelSizeStats, schedulerStatus, status);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                JobStats other = (JobStats) obj;
                return Objects.equals(jobId, other.jobId)
                        && Objects.equals(this.dataCounts, other.dataCounts)
                        && Objects.equals(this.modelSizeStats, other.modelSizeStats)
                        && Objects.equals(this.status, other.status)
                        && Objects.equals(this.schedulerStatus, other.schedulerStatus);
            }
        }

        private QueryPage<JobStats> jobsStats;

        public Response(QueryPage<JobStats> jobsStats) {
            this.jobsStats = jobsStats;
        }

        public Response() {}

        public QueryPage<JobStats> getResponse() {
            return jobsStats;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobsStats = new QueryPage<>(in, JobStats::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            jobsStats.writeTo(out);
        }

        @Override
        public RestStatus status() {
            return jobsStats.count() == 0 ? RestStatus.NOT_FOUND : RestStatus.OK;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return jobsStats.doXContentBody(builder, params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobsStats);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Response other = (Response) obj;
            return Objects.equals(jobsStats, other.jobsStats);
        }

        @SuppressWarnings("deprecation")
        @Override
        public final String toString() {
            try {
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.prettyPrint();
                builder.startObject();
                toXContent(builder, EMPTY_PARAMS);
                builder.endObject();
                return builder.string();
            } catch (Exception e) {
                // So we have a stack trace logged somewhere
                return "{ \"error\" : \"" + org.elasticsearch.ExceptionsHelper.detailedMessage(e) + "\"}";
            }
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final ClusterService clusterService;
        private final JobManager jobManager;
        private final AutodetectProcessManager processManager;
        private final JobProvider jobProvider;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool, ActionFilters actionFilters,
                               ClusterService clusterService, IndexNameExpressionResolver indexNameExpressionResolver,
                               JobManager jobManager, AutodetectProcessManager processManager, JobProvider jobProvider) {
            super(settings, GetJobsStatsAction.NAME, false, threadPool, transportService, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.clusterService = clusterService;
            this.jobManager = jobManager;
            this.processManager = processManager;
            this.jobProvider = jobProvider;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            logger.debug("Get stats for job '{}'", request.getJobId());

            List<Response.JobStats> jobsStats = new ArrayList<>();
            QueryPage<Job> jobs = jobManager.getJob(request.getJobId(), clusterService.state());
            PrelertMetadata prelertMetadata = clusterService.state().metaData().custom(PrelertMetadata.TYPE);
            for (Job job : jobs.results()) {
                DataCounts dataCounts = readDataCounts(job.getId());
                ModelSizeStats modelSizeStats = readModelSizeStats(job.getId());
                JobStatus status = prelertMetadata.getAllocations().get(job.getId()).getStatus();
                Optional<SchedulerStatus> schedulerStatus = prelertMetadata.getSchedulerStatusByJobId(job.getId());
                jobsStats.add(new Response.JobStats(job.getId(), dataCounts, modelSizeStats, status, schedulerStatus.orElse(null)));
            }

            QueryPage<Response.JobStats> jobsStatsPage = new QueryPage<>(jobsStats, jobsStats.size(), Job.RESULTS_FIELD);
            listener.onResponse(new GetJobsStatsAction.Response(jobsStatsPage));
        }

        private DataCounts readDataCounts(String jobId) {
            Optional<DataCounts> counts = processManager.getDataCounts(jobId);
            return counts.orElseGet(() -> jobProvider.dataCounts(jobId));
        }

        private ModelSizeStats readModelSizeStats(String jobId) {
            Optional<ModelSizeStats> sizeStats = processManager.getModelSizeStats(jobId);
            return sizeStats.orElseGet(() -> jobProvider.modelSizeStats(jobId).orElse(null));
        }
    }
}
