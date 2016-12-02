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
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.manager.JobManager;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.scheduler.ScheduledJobRunner;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Objects;

public class StartJobSchedulerAction
extends Action<StartJobSchedulerAction.Request, StartJobSchedulerAction.Response, StartJobSchedulerAction.RequestBuilder> {

    public static final StartJobSchedulerAction INSTANCE = new StartJobSchedulerAction();
    public static final String NAME = "cluster:admin/prelert/job/scheduler/run";

    private StartJobSchedulerAction() {
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

    public static class Request extends ActionRequest implements ToXContent {

        public static ObjectParser<Request, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(NAME, Request::new);

        static {
            PARSER.declareString((request, jobId) -> request.jobId = jobId, Job.ID);
            PARSER.declareObject((request, schedulerState) -> request.schedulerState = schedulerState, SchedulerState.PARSER,
                    SchedulerState.TYPE_FIELD);
        }

        public static Request parseRequest(String jobId, XContentParser parser, ParseFieldMatcherSupplier parseFieldMatcherSupplier) {
            Request request = PARSER.apply(parser, parseFieldMatcherSupplier);
            if (jobId != null) {
                request.jobId = jobId;
            }
            return request;
        }

        private String jobId;
        // TODO (norelease): instead of providing a scheduler state, the user should just provide: startTimeMillis and endTimeMillis
        // the state is useless here as it should always be STARTING
        private SchedulerState schedulerState;

        public Request(String jobId, SchedulerState schedulerState) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, Job.ID.getPreferredName());
            this.schedulerState = ExceptionsHelper.requireNonNull(schedulerState, SchedulerState.TYPE_FIELD.getPreferredName());
            if (schedulerState.getStatus() != JobSchedulerStatus.STARTED) {
                throw new IllegalStateException(
                        "Start job scheduler action requires the scheduler status to be [" + JobSchedulerStatus.STARTED + "]");
            }
        }

        Request() {
        }

        public String getJobId() {
            return jobId;
        }

        public SchedulerState getSchedulerState() {
            return schedulerState;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public Task createTask(long id, String type, String action, TaskId parentTaskId) {
            return new SchedulerTask(id, type, action, parentTaskId, jobId);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            schedulerState = new SchedulerState(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            schedulerState.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Job.ID.getPreferredName(), jobId);
            builder.field(SchedulerState.TYPE_FIELD.getPreferredName(), schedulerState);
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, schedulerState);
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
            return Objects.equals(jobId, other.jobId) && Objects.equals(schedulerState, other.schedulerState);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, StartJobSchedulerAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse {

        Response() {
        }

    }

    public static class SchedulerTask extends CancellableTask {

        private volatile ScheduledJobRunner.Holder holder;

        public SchedulerTask(long id, String type, String action, TaskId parentTaskId, String jobId) {
            super(id, type, action, "job-scheduler-" + jobId, parentTaskId);
        }

        public void setHolder(ScheduledJobRunner.Holder holder) {
            this.holder = holder;
        }

        @Override
        protected void onCancelled() {
            stop();
        }

        /* public for testing */
        public void stop() {
            if (holder != null) {
                holder.stop();
            }
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final JobManager jobManager;
        private final ScheduledJobRunner scheduledJobRunner;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               JobManager jobManager, ScheduledJobRunner scheduledJobRunner) {
            super(settings, StartJobSchedulerAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                    Request::new);
            this.jobManager = jobManager;
            this.scheduledJobRunner = scheduledJobRunner;
        }

        @Override
        protected void doExecute(Task task, Request request, ActionListener<Response> listener) {
            SchedulerTask schedulerTask = (SchedulerTask) task;
            Job job = jobManager.getJobOrThrowIfUnknown(request.jobId);
            Allocation allocation = jobManager.getJobAllocation(job.getId());
            scheduledJobRunner.run(job, request.getSchedulerState(), allocation, schedulerTask, (error) -> {
                if (error != null) {
                    listener.onFailure(error);
                } else {
                    listener.onResponse(new Response());
                }
            });
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            throw new UnsupportedOperationException("the task parameter is required");
        }
    }
}
