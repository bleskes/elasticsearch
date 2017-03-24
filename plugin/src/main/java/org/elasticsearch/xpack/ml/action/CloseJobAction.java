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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.MlMetadata;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData.PersistentTask;
import org.elasticsearch.xpack.persistent.RemovePersistentTaskAction;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CloseJobAction extends Action<CloseJobAction.Request, CloseJobAction.Response, CloseJobAction.RequestBuilder> {

    public static final CloseJobAction INSTANCE = new CloseJobAction();
    public static final String NAME = "cluster:admin/ml/job/close";

    private CloseJobAction() {
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

    public static class Request extends MasterNodeRequest<Request> implements ToXContent {

        public static final ParseField TIMEOUT = new ParseField("timeout");
        public static final ParseField FORCE = new ParseField("force");
        public static ObjectParser<Request, Void> PARSER = new ObjectParser<>(NAME, Request::new);

        static {
            PARSER.declareString(Request::setJobId, Job.ID);
            PARSER.declareString((request, val) ->
                    request.setTimeout(TimeValue.parseTimeValue(val, TIMEOUT.getPreferredName())), TIMEOUT);
            PARSER.declareBoolean(Request::setForce, FORCE);
        }

        public static Request parseRequest(String jobId, XContentParser parser) {
            Request request = PARSER.apply(parser, null);
            if (jobId != null) {
                request.jobId = jobId;
            }
            return request;
        }

        private String jobId;
        private TimeValue timeout = TimeValue.timeValueMinutes(20);
        private boolean force = false;

        Request() {}

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, Job.ID.getPreferredName());
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public TimeValue getTimeout() {
            return timeout;
        }

        public void setTimeout(TimeValue timeout) {
            this.timeout = timeout;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            timeout = new TimeValue(in);
            force = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            timeout.writeTo(out);
            out.writeBoolean(force);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Job.ID.getPreferredName(), jobId);
            builder.field(TIMEOUT.getPreferredName(), timeout.getStringRep());
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, timeout);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(jobId, other.jobId) &&
                    Objects.equals(timeout, other.timeout) &&
                    Objects.equals(force, other.force);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client, CloseJobAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements ToXContentObject {

        private boolean closed;

        Response() {
        }

        Response(boolean closed) {
            this.closed = closed;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            closed = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBoolean(closed);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("closed", closed);
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return closed == response.closed;
        }

        @Override
        public int hashCode() {
            return Objects.hash(closed);
        }
    }

    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final ClusterService clusterService;
        private final CloseJobService closeJobService;
        private final Client client;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               ClusterService clusterService, CloseJobService closeJobService, Client client) {
            super(settings, CloseJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.clusterService = clusterService;
            this.closeJobService = closeJobService;
            this.client = client;
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
            if (request.isForce()) {
                forceCloseJob(client, request.getJobId(), state, listener);
            } else {
                closeJob(request, listener);
            }
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }

        private void closeJob(Request request, ActionListener<Response> listener) {
            clusterService.submitStateUpdateTask("closing job [" + request.getJobId() + "]", new ClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) throws Exception {
                    return moveJobToClosingState(request.getJobId(), currentState);
                }

                @Override
                public void onFailure(String source, Exception e) {
                    listener.onFailure(e);
                }

                @Override
                public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                    threadPool.executor(ThreadPool.Names.GENERIC).execute(new AbstractRunnable() {
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }

                        @Override
                        protected void doRun() throws Exception {
                            closeJobService.closeJob(request, listener);
                        }
                    });
                }
            });
        }

        private void forceCloseJob(Client client, String jobId, ClusterState currentState,
                                   ActionListener<Response> listener) {
            PersistentTask<?> task = MlMetadata.getJobTask(jobId,
                    currentState.getMetaData().custom(PersistentTasksCustomMetaData.TYPE));
            if (task != null) {
                RemovePersistentTaskAction.Request request = new RemovePersistentTaskAction.Request(task.getId());
                client.execute(RemovePersistentTaskAction.INSTANCE, request,
                        ActionListener.wrap(
                                response -> listener.onResponse(new Response(response.isAcknowledged())),
                                listener::onFailure));
            } else {
                String msg = "Requested job [" + jobId + "] be force-closed, but job's task" +
                        "could not be found.";
                logger.warn(msg);
                listener.onFailure(new RuntimeException(msg));
            }
        }

    }

    static PersistentTask<?> validateAndFindTask(String jobId, ClusterState state) {
        MlMetadata mlMetadata = state.metaData().custom(MlMetadata.TYPE);
        if (mlMetadata.getJobs().containsKey(jobId) == false) {
            throw ExceptionsHelper.missingJobException(jobId);
        }

        PersistentTasksCustomMetaData tasks = state.getMetaData().custom(PersistentTasksCustomMetaData.TYPE);
        Optional<DatafeedConfig> datafeed = mlMetadata.getDatafeedByJobId(jobId);
        if (datafeed.isPresent()) {
            DatafeedState datafeedState = MlMetadata.getDatafeedState(datafeed.get().getId(), tasks);
            if (datafeedState != DatafeedState.STOPPED) {
                throw new ElasticsearchStatusException("cannot close job [{}], datafeed hasn't been stopped",
                        RestStatus.CONFLICT, jobId);
            }
        }

        PersistentTask<?> jobTask = MlMetadata.getJobTask(jobId, tasks);
        if (jobTask != null) {
            JobState jobState = (JobState) jobTask.getStatus();
            if (jobState.isAnyOf(JobState.OPENED, JobState.FAILED) == false) {
                throw new ElasticsearchStatusException("cannot close job [{}], expected job state [{}], but got [{}]",
                        RestStatus.CONFLICT, jobId, JobState.OPENED, jobState);
            }
            return jobTask;
        }
        throw new ElasticsearchStatusException("cannot close job [{}], expected job state [{}], but got [{}]",
                RestStatus.CONFLICT, jobId, JobState.OPENED, JobState.CLOSED);
    }

    static ClusterState moveJobToClosingState(String jobId, ClusterState currentState) {
        PersistentTask<?> task = validateAndFindTask(jobId, currentState);
        PersistentTasksCustomMetaData currentTasks = currentState.getMetaData().custom(PersistentTasksCustomMetaData.TYPE);
        Map<Long, PersistentTask<?>> updatedTasks = new HashMap<>(currentTasks.taskMap());
        PersistentTask<?> taskToUpdate = currentTasks.getTask(task.getId());
        taskToUpdate = new PersistentTask<>(taskToUpdate, JobState.CLOSING);
        updatedTasks.put(taskToUpdate.getId(), taskToUpdate);
        PersistentTasksCustomMetaData newTasks = new PersistentTasksCustomMetaData(currentTasks.getCurrentId(), updatedTasks);

        MlMetadata mlMetadata = currentState.metaData().custom(MlMetadata.TYPE);
        Job.Builder jobBuilder = new Job.Builder(mlMetadata.getJobs().get(jobId));
        jobBuilder.setFinishedTime(new Date());
        MlMetadata.Builder mlMetadataBuilder = new MlMetadata.Builder(mlMetadata);
        mlMetadataBuilder.putJob(jobBuilder.build(), true);

        ClusterState.Builder builder = ClusterState.builder(currentState);
        return builder
                .metaData(new MetaData.Builder(currentState.metaData())
                        .putCustom(MlMetadata.TYPE, mlMetadataBuilder.build())
                        .putCustom(PersistentTasksCustomMetaData.TYPE, newTasks))
                .build();
    }
}

