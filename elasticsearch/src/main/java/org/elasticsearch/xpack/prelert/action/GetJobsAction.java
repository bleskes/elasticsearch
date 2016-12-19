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
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerStatus;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.manager.AutodetectProcessManager;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class GetJobsAction extends Action<GetJobsAction.Request, GetJobsAction.Response, GetJobsAction.RequestBuilder> {

    public static final GetJobsAction INSTANCE = new GetJobsAction();
    public static final String NAME = "cluster:admin/prelert/jobs/get";

    private static final String ALL = "_all";
    private static final String CONFIG = "config";
    private static final String DATA_COUNTS = "data_counts";
    private static final String MODEL_SIZE_STATS = "model_size_stats";
    private static final String SCHEDULER_STATUS = "scheduler_status";
    private static final String STATUS = "status";

    private static final List<String> METRIC_WHITELIST = Arrays.asList(ALL, CONFIG, DATA_COUNTS,
            MODEL_SIZE_STATS, SCHEDULER_STATUS, STATUS);

    private GetJobsAction() {
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

        public static final ObjectParser<Request, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(NAME, Request::new);
        public static final ParseField METRIC = new ParseField("metric");

        static {
            PARSER.declareString(Request::setJobId, Job.ID);
            PARSER.declareObject(Request::setPageParams, PageParams.PARSER, PageParams.PAGE);
            PARSER.declareString((request, metric) -> {
                Set<String> stats = Strings.splitStringByCommaToSet(metric);
                request.setStats(stats);
            }, METRIC);
        }

        private String jobId = null;
        private PageParams pageParams = null;
        private boolean config;
        private boolean dataCounts;
        private boolean modelSizeStats;
        private boolean schedulerStatus;
        private boolean status;


        public Request() {
            config = true;
        }

        public void setJobId(String jobId) {
            if (pageParams != null) {
                throw new IllegalArgumentException("Cannot set [from, size] when getting a single job.");
            }
            this.jobId = jobId;
        }

        public String getJobId() {
            return jobId;
        }

        public PageParams getPageParams() {
            return pageParams;
        }

        public void setPageParams(PageParams pageParams) {
            if (jobId != null) {
                throw new IllegalArgumentException("Cannot set [jobId] when getting multiple jobs.");
            }
            this.pageParams = ExceptionsHelper.requireNonNull(pageParams, PageParams.PAGE.getPreferredName());
        }

        public Request all() {
            config = true;
            dataCounts = true;
            modelSizeStats = true;
            schedulerStatus = true;
            status = true;
            return this;
        }

        public boolean config() {
            return config;
        }

        public Request config(boolean config) {
            this.config = config;
            return this;
        }

        public boolean dataCounts() {
            return dataCounts;
        }

        public Request dataCounts(boolean dataCounts) {
            this.dataCounts = dataCounts;
            return this;
        }

        public boolean modelSizeStats() {
            return modelSizeStats;
        }

        public Request modelSizeStats(boolean modelSizeStats) {
            this.modelSizeStats = modelSizeStats;
            return this;
        }

        public boolean schedulerStatus() {
            return schedulerStatus;
        }

        public Request schedulerStatus(boolean schedulerStatus) {
            this.schedulerStatus = schedulerStatus;
            return this;
        }

        public void setStats(Set<String> stats) {
            for (String s : stats) {
                if (!METRIC_WHITELIST.contains(s)) {
                    throw new ElasticsearchStatusException("Metric [" + s + "] is not a valid metric.  "
                            + "Accepted metrics are: [" + METRIC_WHITELIST + "]", RestStatus.BAD_REQUEST);
                }
            }
            if (stats.contains(ALL)) {
                all();
            } else {
                config(stats.contains(CONFIG));
                dataCounts(stats.contains(DATA_COUNTS));
                modelSizeStats(stats.contains(MODEL_SIZE_STATS));
                schedulerStatus(stats.contains(SCHEDULER_STATUS));
                status(stats.contains(STATUS));
            }
        }

        public boolean status() {
            return status;
        }

        public Request status(boolean status) {
            this.status = status;
            return this;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readOptionalString();
            config = in.readBoolean();
            dataCounts = in.readBoolean();
            modelSizeStats = in.readBoolean();
            schedulerStatus = in.readBoolean();
            status = in.readBoolean();
            pageParams = in.readOptionalWriteable(PageParams::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalString(jobId);
            out.writeBoolean(config);
            out.writeBoolean(dataCounts);
            out.writeBoolean(modelSizeStats);
            out.writeBoolean(schedulerStatus);
            out.writeBoolean(status);
            out.writeOptionalWriteable(pageParams);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, config, dataCounts, modelSizeStats, schedulerStatus, status, pageParams);
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
            return Objects.equals(jobId, other.jobId)
                    && this.config == other.config
                    && this.dataCounts == other.dataCounts
                    && this.modelSizeStats == other.modelSizeStats
                    && this.schedulerStatus == other.schedulerStatus
                    && this.status == other.status
                    && Objects.equals(this.pageParams, other.pageParams);
        }
    }

    public static class RequestBuilder extends MasterNodeReadOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetJobsAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        public static class JobInfo implements ToXContent, Writeable {
            private final String jobId;
            @Nullable
            private Job jobConfig;
            @Nullable
            private DataCounts dataCounts;
            @Nullable
            private ModelSizeStats modelSizeStats;
            @Nullable
            private SchedulerStatus schedulerStatus;
            @Nullable
            private JobStatus status;



            JobInfo(String jobId, @Nullable Job job, @Nullable DataCounts dataCounts, @Nullable ModelSizeStats modelSizeStats,
                    @Nullable SchedulerStatus schedulerStatus, @Nullable JobStatus status) {
                this.jobId = jobId;
                this.jobConfig = job;
                this.dataCounts = dataCounts;
                this.modelSizeStats = modelSizeStats;
                this.schedulerStatus = schedulerStatus;
                this.status = status;
            }

            JobInfo(StreamInput in) throws IOException {
                jobId = in.readString();
                jobConfig = in.readOptionalWriteable(Job::new);
                dataCounts = in.readOptionalWriteable(DataCounts::new);
                modelSizeStats = in.readOptionalWriteable(ModelSizeStats::new);
                schedulerStatus = in.readOptionalWriteable(SchedulerStatus::fromStream);
                status = in.readOptionalWriteable(JobStatus::fromStream);
            }

            public String getJobid() {
                return jobId;
            }

            public Job getJobConfig() {
                return jobConfig;
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
                if (jobConfig != null) {
                    builder.field(CONFIG, jobConfig);
                }
                if (dataCounts != null) {
                    builder.field(DATA_COUNTS, dataCounts);
                }
                if (modelSizeStats != null) {
                    builder.field(MODEL_SIZE_STATS, modelSizeStats);
                }
                if (schedulerStatus != null) {
                    builder.field(SCHEDULER_STATUS, schedulerStatus);
                }
                if (status != null) {
                    builder.field(STATUS, status);
                }
                builder.endObject();

                return builder;
            }

            @Override
            public void writeTo(StreamOutput out) throws IOException {
                out.writeString(jobId);
                out.writeOptionalWriteable(jobConfig);
                out.writeOptionalWriteable(dataCounts);
                out.writeOptionalWriteable(modelSizeStats);
                out.writeOptionalWriteable(schedulerStatus);
                out.writeOptionalWriteable(status);
            }

            @Override
            public int hashCode() {
                return Objects.hash(jobId, jobConfig, dataCounts, modelSizeStats, schedulerStatus, status);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                JobInfo other = (JobInfo) obj;
                return Objects.equals(jobId, other.jobId)
                        && Objects.equals(jobConfig, other.jobConfig)
                        && Objects.equals(this.dataCounts, other.dataCounts)
                        && Objects.equals(this.modelSizeStats, other.modelSizeStats)
                        && Objects.equals(this.schedulerStatus, other.schedulerStatus)
                        && Objects.equals(this.status, other.status);
            }
        }

        private QueryPage<JobInfo> jobs;

        public Response(QueryPage<JobInfo> jobs) {
            this.jobs = jobs;
        }

        public Response() {}

        public QueryPage<JobInfo> getResponse() {
            return jobs;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobs = new QueryPage<>(in, JobInfo::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            jobs.writeTo(out);
        }

        @Override
        public RestStatus status() {
            return jobs.count() == 0 ? RestStatus.NOT_FOUND : RestStatus.OK;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return jobs.doXContentBody(builder, params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobs);
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
            return Objects.equals(jobs, other.jobs);
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


    public static class TransportAction extends TransportMasterNodeReadAction<Request, Response> {

        private final JobManager jobManager;
        private final AutodetectProcessManager processManager;
        private final JobProvider jobProvider;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                ThreadPool threadPool, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                JobManager jobManager, AutodetectProcessManager processManager, JobProvider jobProvider) {
            super(settings, GetJobsAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.jobManager = jobManager;
            this.processManager = processManager;
            this.jobProvider = jobProvider;
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
            logger.debug("Get job '{}', config={}, data_counts={}, model_size_stats={}",
                    request.getJobId(), request.config(), request.dataCounts(), request.modelSizeStats());

            QueryPage<Response.JobInfo> response;

            // Single Job
            if (request.jobId != null && !request.jobId.isEmpty()) {
                // always get the job regardless of the request.config param because if the job
                // can't be found a different response is returned.
                QueryPage<Job> jobs = jobManager.getJob(request.getJobId(), state);
                if (jobs.count() == 0) {
                    logger.debug(String.format(Locale.ROOT, "Cannot find job '%s'", request.getJobId()));
                    throw QueryPage.emptyQueryPage(Job.RESULTS_FIELD);
                } else if (jobs.count() > 1) {
                    logger.error("More than one job found for {} [{}]", Job.ID.getPreferredName(), request.getJobId());
                }

                logger.debug("Returning job [" + request.getJobId() + "]");
                Job jobConfig = request.config() ? jobs.results().get(0) : null;
                DataCounts dataCounts = readDataCounts(request.dataCounts(), request.getJobId());
                ModelSizeStats modelSizeStats = readModelSizeStats(request.modelSizeStats(), request.getJobId());
                SchedulerStatus schedulerStatus = readSchedulerStatus(request.schedulerStatus(), request.getJobId());
                JobStatus jobStatus = readJobStatus(request.status(), request.getJobId());

                Response.JobInfo jobInfo = new Response.JobInfo(
                        request.getJobId(), jobConfig, dataCounts, modelSizeStats, schedulerStatus, jobStatus);
                response = new QueryPage<>(Collections.singletonList(jobInfo), 1, Job.RESULTS_FIELD);

            } else if (request.getPageParams() != null) {
                // Multiple Jobs

                PageParams pageParams = request.getPageParams();
                QueryPage<Job> jobsPage = jobManager.getJobs(pageParams.getFrom(), pageParams.getSize(), state);
                List<Response.JobInfo> jobInfoList = new ArrayList<>();
                for (Job job : jobsPage.results()) {
                    Job jobConfig = request.config() ? job : null;
                    DataCounts dataCounts = readDataCounts(request.dataCounts(), job.getId());
                    ModelSizeStats modelSizeStats = readModelSizeStats(request.modelSizeStats(), job.getId());
                    SchedulerStatus schedulerStatus = readSchedulerStatus(request.schedulerStatus(), job.getId());
                    JobStatus jobStatus = readJobStatus(request.status(), job.getId());
                    Response.JobInfo jobInfo = new Response.JobInfo(job.getId(), jobConfig, dataCounts, modelSizeStats,
                            schedulerStatus, jobStatus);
                    jobInfoList.add(jobInfo);
                }
                response = new QueryPage<>(jobInfoList, jobsPage.count(), Job.RESULTS_FIELD);
            } else {
                throw new IllegalStateException("Both jobId and pageParams are null");
            }

            listener.onResponse(new Response(response));
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
        }

        private DataCounts readDataCounts(boolean dataCounts, String jobId) {
            if (dataCounts) {
                Optional<DataCounts> counts = processManager.getDataCounts(jobId);
                return counts.orElseGet(() -> jobProvider.dataCounts(jobId));
            }
            return null;
        }

        private ModelSizeStats readModelSizeStats(boolean modelSizeStats, String jobId) {
            if (modelSizeStats) {
                Optional<ModelSizeStats> sizeStats = processManager.getModelSizeStats(jobId);
                return sizeStats.orElseGet(() -> jobProvider.modelSizeStats(jobId).orElse(null));
            }
            return null;
        }

        private SchedulerStatus readSchedulerStatus(boolean schedulerState, String jobId) {
            return schedulerState ? jobManager.getSchedulerStatus(jobId).orElse(null) : null;
        }

        private JobStatus readJobStatus(boolean status, String jobId) {
            return status ? jobManager.getJobStatus(jobId) : null;
        }
    }

}
