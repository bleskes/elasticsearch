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
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.Version;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.NodeClosedException;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.MlMetadata;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.persistence.JobStorageDeletionTask;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.xpack.persistent.PersistentTasksService;
import org.elasticsearch.xpack.security.InternalClient;

import java.io.IOException;
import java.util.Objects;

public class DeleteJobAction extends Action<DeleteJobAction.Request, DeleteJobAction.Response, DeleteJobAction.RequestBuilder> {

    public static final DeleteJobAction INSTANCE = new DeleteJobAction();
    public static final String NAME = "cluster:admin/xpack/ml/job/delete";

    private DeleteJobAction() {
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

    public static class Request extends AcknowledgedRequest<Request> {

        private String jobId;
        private boolean force;

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, Job.ID.getPreferredName());
        }

        Request() {}

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
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
        public Task createTask(long id, String type, String action, TaskId parentTaskId) {
            return new JobStorageDeletionTask(id, type, action, "delete-job-" + jobId, parentTaskId);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            if (in.getVersion().onOrAfter(Version.V_5_5_0_UNRELEASED)) {
                force = in.readBoolean();
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            if (out.getVersion().onOrAfter(Version.V_5_5_0_UNRELEASED)) {
                out.writeBoolean(force);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, force);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            DeleteJobAction.Request other = (DeleteJobAction.Request) obj;
            return Objects.equals(jobId, other.jobId) && Objects.equals(force, other.force);
        }
    }

    static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client, DeleteJobAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse {

        public Response(boolean acknowledged) {
            super(acknowledged);
        }

        private Response() {}

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            readAcknowledged(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            writeAcknowledged(out);
        }
    }

    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final InternalClient internalClient;
        private final JobManager jobManager;
        private final PersistentTasksService persistentTasksService;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService, ThreadPool threadPool,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               JobManager jobManager, PersistentTasksService persistentTasksService, InternalClient internalClient) {
            super(settings, DeleteJobAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.internalClient = internalClient;
            this.jobManager = jobManager;
            this.persistentTasksService = persistentTasksService;
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
        protected void masterOperation(Task task, Request request, ClusterState state, ActionListener<Response> listener) throws Exception {

            // For a normal delete check if the job is already being deleted.
            if (request.isForce() == false) {
                MlMetadata currentMlMetadata = state.metaData().custom(MlMetadata.TYPE);
                if (currentMlMetadata != null) {
                    Job job = currentMlMetadata.getJobs().get(request.getJobId());
                    if (job != null && job.isDeleted()) {
                        // This is a generous timeout value but it's unlikely to ever take this long
                        waitForDeletingJob(request.getJobId(), MachineLearning.STATE_PERSIST_RESTORE_TIMEOUT, listener);
                        return;
                    }
                }
            }

            ActionListener<Boolean> markAsDeletingListener = ActionListener.wrap(
                    response -> {
                        if (request.isForce()) {
                            forceDeleteJob(request, (JobStorageDeletionTask) task, listener);
                        } else {
                            normalDeleteJob(request, (JobStorageDeletionTask) task, listener);
                        }
                    },
                    listener::onFailure);

            markJobAsDeleting(request.getJobId(), markAsDeletingListener, request.isForce());
        }

        @Override
        protected void masterOperation(Request request, ClusterState state, ActionListener<Response> listener) throws Exception {
            throw new UnsupportedOperationException("the Task parameter is required");
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }

        private void normalDeleteJob(Request request, JobStorageDeletionTask task, ActionListener<Response> listener) {
            jobManager.deleteJob(request, task, listener);
        }

        private void forceDeleteJob(Request request, JobStorageDeletionTask task, ActionListener<Response> listener) {

            final ClusterState state = clusterService.state();
            final String jobId = request.getJobId();

            // 3. Delete the job
            ActionListener<Boolean> removeTaskListener = new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean response) {
                    jobManager.deleteJob(request, task, listener);
                }

                @Override
                public void onFailure(Exception e) {
                    if (e instanceof ResourceNotFoundException) {
                        jobManager.deleteJob(request, task, listener);
                    } else {
                        listener.onFailure(e);
                    }
                }
            };

            // 2. Cancel the persistent task. This closes the process gracefully so
            // the process should be killed first.
            ActionListener<KillProcessAction.Response> killJobListener = ActionListener.wrap(
                    response -> {
                        removePersistentTask(request.getJobId(), state, removeTaskListener);
                    },
                    e -> {
                        if (e instanceof ElasticsearchStatusException) {
                            // Killing the process marks the task as completed so it
                            // may have disappeared when we get here
                            removePersistentTask(request.getJobId(), state, removeTaskListener);
                        } else {
                            listener.onFailure(e);
                        }
                    }
            );

            // 1. Kill the job's process
            killProcess(jobId, killJobListener);
        }

        private void killProcess(String jobId, ActionListener<KillProcessAction.Response> listener) {
            KillProcessAction.Request killRequest = new KillProcessAction.Request(jobId);
            internalClient.execute(KillProcessAction.INSTANCE, killRequest, listener);
        }

        private void removePersistentTask(String jobId, ClusterState currentState,
                                          ActionListener<Boolean> listener) {
            PersistentTasksCustomMetaData tasks = currentState.getMetaData().custom(PersistentTasksCustomMetaData.TYPE);

            PersistentTasksCustomMetaData.PersistentTask<?> jobTask = MlMetadata.getJobTask(jobId, tasks);
            if (jobTask == null) {
                listener.onResponse(null);
            } else {
                persistentTasksService.cancelPersistentTask(jobTask.getId(),
                        new ActionListener<PersistentTasksCustomMetaData.PersistentTask<?>>() {
                            @Override
                            public void onResponse(PersistentTasksCustomMetaData.PersistentTask<?> task) {
                                listener.onResponse(Boolean.TRUE);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                listener.onFailure(e);
                            }
                        });
            }
        }

        void markJobAsDeleting(String jobId, ActionListener<Boolean> listener, boolean force) {
            clusterService.submitStateUpdateTask("mark-job-as-deleted", new ClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) throws Exception {
                    MlMetadata currentMlMetadata = currentState.metaData().custom(MlMetadata.TYPE);
                    PersistentTasksCustomMetaData tasks = currentState.metaData().custom(PersistentTasksCustomMetaData.TYPE);
                    MlMetadata.Builder builder = new MlMetadata.Builder(currentMlMetadata);
                    builder.markJobAsDeleted(jobId, tasks, force);
                    return buildNewClusterState(currentState, builder);
                }

                @Override
                public void onFailure(String source, Exception e) {
                    listener.onFailure(e);
                }

                @Override
                public void clusterStatePublished(ClusterChangedEvent clusterChangedEvent) {
                    logger.debug("Job [" + jobId + "] is successfully marked as deleted");
                    listener.onResponse(true);
                }
            });
        }

        void waitForDeletingJob(String jobId, TimeValue timeout, ActionListener<Response> listener) {
            ClusterStateObserver stateObserver = new ClusterStateObserver(clusterService, timeout, logger, threadPool.getThreadContext());

            ClusterState clusterState = stateObserver.setAndGetObservedState();
            if (jobIsDeletedFromState(jobId, clusterState)) {
                listener.onResponse(new Response(true));
            } else {
                stateObserver.waitForNextChange(new ClusterStateObserver.Listener() {
                    @Override
                    public void onNewClusterState(ClusterState state) {
                        listener.onResponse(new Response(true));
                    }

                    @Override
                    public void onClusterServiceClose() {
                        listener.onFailure(new NodeClosedException(clusterService.localNode()));
                    }

                    @Override
                    public void onTimeout(TimeValue timeout) {
                        listener.onFailure(new IllegalStateException("timed out after " + timeout));
                    }
                }, newClusterState -> jobIsDeletedFromState(jobId, newClusterState), timeout);
            }
        }

        static boolean jobIsDeletedFromState(String jobId, ClusterState clusterState) {
            MlMetadata metadata = clusterState.metaData().custom(MlMetadata.TYPE);
            if (metadata == null) {
                return true;
            }
            return !metadata.getJobs().containsKey(jobId);
        }

        private static ClusterState buildNewClusterState(ClusterState currentState, MlMetadata.Builder builder) {
            ClusterState.Builder newState = ClusterState.builder(currentState);
            newState.metaData(MetaData.builder(currentState.getMetaData()).putCustom(MlMetadata.TYPE, builder.build()).build());
            return newState.build();
        }
    }
}
