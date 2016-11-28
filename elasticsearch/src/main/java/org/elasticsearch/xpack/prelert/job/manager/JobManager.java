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
package org.elasticsearch.xpack.prelert.job.manager;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.action.PauseJobAction;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.ResumeJobAction;
import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.StopJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.UpdateJobSchedulerStatusAction;
import org.elasticsearch.xpack.prelert.action.UpdateJobStatusAction;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Allows interactions with jobs. The managed interactions include:
 * <ul>
 * <li>creation</li>
 * <li>deletion</li>
 * <li>updating</li>
 * <li>starting/stopping of scheduled jobs</li>
 * </ul>
 */
public class JobManager extends AbstractComponent {

    private static final Logger LOGGER = Loggers.getLogger(JobManager.class);

    /**
     * Field name in which to store the API version in the usage info
     */
    public static final String APP_VER_FIELDNAME = "appVer";

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY.getPreferredName();
    private final JobProvider jobProvider;
    private final ClusterService clusterService;
    private final JobResultsPersister jobResultsPersister;
    private final JobDataCountsPersister jobDataCountsPersister;


    /**
     * Create a JobManager
     */
    public JobManager(Settings settings, JobProvider jobProvider, JobResultsPersister jobResultsPersister,
                      JobDataCountsPersister jobDataCountsPersister, ClusterService clusterService) {
        super(settings);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.clusterService = clusterService;
        this.jobResultsPersister = jobResultsPersister;
        this.jobDataCountsPersister = jobDataCountsPersister;
    }

    /**
     * Get the details of the specific job wrapped in a <code>Optional</code>
     *
     * @param jobId
     *            the jobId
     * @return An {@code Optional} containing the {@code Job} if a job
     *         with the given {@code jobId} exists, or an empty {@code Optional}
     *         otherwise
     */
    public QueryPage<Job> getJob(String jobId, ClusterState clusterState) {
        PrelertMetadata prelertMetadata = clusterState.getMetaData().custom(PrelertMetadata.TYPE);
        Job job = prelertMetadata.getJobs().get(jobId);
        if (job == null) {
            throw QueryPage.emptyQueryPage(Job.RESULTS_FIELD);
        }

        return new QueryPage<>(Collections.singletonList(job), 1, Job.RESULTS_FIELD);
    }

    /**
     * Get details of all Jobs.
     *
     * @param from
     *            Skip the first N Jobs. This parameter is for paging results if
     *            not required set to 0.
     * @param size
     *            Take only this number of Jobs
     * @return A query page object with hitCount set to the total number of jobs
     *         not the only the number returned here as determined by the
     *         <code>size</code> parameter.
     */
    public QueryPage<Job> getJobs(int from, int size, ClusterState clusterState) {
        PrelertMetadata prelertMetadata = clusterState.getMetaData().custom(PrelertMetadata.TYPE);
        List<Job> jobs = prelertMetadata.getJobs().entrySet().stream()
                .skip(from)
                .limit(size)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return new QueryPage<>(jobs, prelertMetadata.getJobs().size(), Job.RESULTS_FIELD);
    }

    /**
     * Returns the non-null {@code Job} object for the given
     * {@code jobId} or throws
     * {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     *            the jobId
     * @return the {@code Job} if a job with the given {@code jobId}
     *         exists
     * @throws org.elasticsearch.ResourceNotFoundException
     *             if there is no job with matching the given {@code jobId}
     */
    public Job getJobOrThrowIfUnknown(String jobId) {
        return getJobOrThrowIfUnknown(clusterService.state(), jobId);
    }

    public Allocation getJobAllocation(String jobId) {
        return getAllocation(clusterService.state(), jobId);
    }

    /**
     * Returns the non-null {@code Job} object for the given
     * {@code jobId} or throws
     * {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     *            the jobId
     * @return the {@code Job} if a job with the given {@code jobId}
     *         exists
     * @throws org.elasticsearch.ResourceNotFoundException
     *             if there is no job with matching the given {@code jobId}
     */
    public Job getJobOrThrowIfUnknown(ClusterState clusterState, String jobId) {
        PrelertMetadata prelertMetadata = clusterState.metaData().custom(PrelertMetadata.TYPE);
        Job job = prelertMetadata.getJobs().get(jobId);
        if (job == null) {
            throw ExceptionsHelper.missingJobException(jobId);
        }
        return job;
    }

    /**
     * Stores a job in the cluster state
     */
    public void putJob(PutJobAction.Request request, ActionListener<PutJobAction.Response> actionListener) {
        Job job = request.getJob();
        ActionListener<Boolean> delegateListener = new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean jobSaved) {
                jobProvider.createJobRelatedIndices(job, new ActionListener<Boolean>() {
                    @Override
                    public void onResponse(Boolean indicesCreated) {
                        // NORELEASE: make auditing async too (we can't do
                        // blocking stuff here):
                        // audit(jobDetails.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

                        // Also I wonder if we need to audit log infra
                        // structure in prelert as when we merge into xpack
                        // we can use its audit trailing. See:
                        // https://github.com/elastic/prelert-legacy/issues/48
                        actionListener.onResponse(new PutJobAction.Response(jobSaved && indicesCreated, job));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        actionListener.onFailure(e);

                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                actionListener.onFailure(e);
            }
        };
        clusterService.submitStateUpdateTask("put-job-" + job.getId(),
                new AckedClusterStateUpdateTask<Boolean>(request, delegateListener) {

            @Override
            protected Boolean newResponse(boolean acknowledged) {
                return acknowledged;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerPutJob(job, request.isOverwrite(), currentState);
            }

        });
    }

    ClusterState innerPutJob(Job job, boolean overwrite, ClusterState currentState) {
        PrelertMetadata currentPrelertMetadata = currentState.metaData().custom(PrelertMetadata.TYPE);
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        builder.putJob(job, overwrite);
        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData()).putCustom(PrelertMetadata.TYPE, builder.build()).build());
        return newState.build();
    }

    /**
     * Deletes a job.
     *
     * The clean-up involves:
     * <ul>
     * <li>Deleting the index containing job results</li>
     * <li>Deleting the job logs</li>
     * <li>Removing the job from the cluster state</li>
     * </ul>
     *
     * @param request
     *            the delete job request
     * @param actionListener
     *            the action listener
     */
    public void deleteJob(DeleteJobAction.Request request, ActionListener<DeleteJobAction.Response> actionListener) {
        String jobId = request.getJobId();
        LOGGER.debug("Deleting job '" + jobId + "'");

        ActionListener<Boolean> delegateListener = new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean jobDeleted) {
                if (jobDeleted) {
                    jobProvider.deleteJobRelatedIndices(request.getJobId(), actionListener);
                    // NORELEASE: This is not the place the audit log
                    // (indexes a document), because this method is
                    // executed on the cluster state update task thread and any
                    // action performed on that thread should be quick.
                    //audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));
                } else {
                    actionListener.onResponse(new DeleteJobAction.Response(false));
                }
            }

            @Override
            public void onFailure(Exception e) {
                actionListener.onFailure(e);
            }
        };

        clusterService.submitStateUpdateTask("delete-job-" + jobId,
                new AckedClusterStateUpdateTask<Boolean>(request, delegateListener) {

            @Override
            protected Boolean newResponse(boolean acknowledged) {
                return acknowledged;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return removeJobFromClusterState(jobId, currentState);
            }
        });
    }

    ClusterState removeJobFromClusterState(String jobId, ClusterState currentState) {
        PrelertMetadata currentPrelertMetadata = currentState.metaData().custom(PrelertMetadata.TYPE);
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        builder.removeJob(jobId);

        Allocation allocation = currentPrelertMetadata.getAllocations().get(jobId);
        if (allocation != null) {
            SchedulerState schedulerState = allocation.getSchedulerState();
            if (schedulerState != null && schedulerState.getStatus() != JobSchedulerStatus.STOPPED) {
                throw ExceptionsHelper.conflictStatusException(Messages.getMessage(Messages.JOB_CANNOT_DELETE_WHILE_SCHEDULER_RUNS, jobId));
            }
            if (!allocation.getStatus().isAnyOf(JobStatus.CLOSED, JobStatus.PAUSED, JobStatus.FAILED)) {
                throw ExceptionsHelper.conflictStatusException(Messages.getMessage(
                        Messages.JOB_CANNOT_DELETE_WHILE_RUNNING, jobId, allocation.getStatus()));
            }
        }
        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData()).putCustom(PrelertMetadata.TYPE, builder.build()).build());
        return newState.build();
    }

    public void startJobScheduler(StartJobSchedulerAction.Request request,
            ActionListener<StartJobSchedulerAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("start-scheduler-job-" + request.getJobId(),
                new AckedClusterStateUpdateTask<StartJobSchedulerAction.Response>(request, actionListener) {

            @Override
            protected StartJobSchedulerAction.Response newResponse(boolean acknowledged) {
                return new StartJobSchedulerAction.Response(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                long startTime = request.getSchedulerState().getStartTimeMillis();
                Long endTime = request.getSchedulerState().getEndTimeMillis();
                return innerUpdateSchedulerState(currentState, request.getJobId(), JobSchedulerStatus.STARTING, startTime, endTime);
            }
        });
    }

    public void stopJobScheduler(StopJobSchedulerAction.Request request, ActionListener<StopJobSchedulerAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("stop-scheduler-job-" + request.getJobId(),
                new AckedClusterStateUpdateTask<StopJobSchedulerAction.Response>(request, actionListener) {

            @Override
            protected StopJobSchedulerAction.Response newResponse(boolean acknowledged) {
                return new StopJobSchedulerAction.Response(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerUpdateSchedulerState(currentState, request.getJobId(), JobSchedulerStatus.STOPPING, null, null);
            }
        });
    }

    private void checkJobIsScheduled(Job job) {
        if (job.getSchedulerConfig() == null) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.JOB_SCHEDULER_NO_SUCH_SCHEDULED_JOB, job.getId()));
        }
    }

    public Optional<SchedulerState> getSchedulerState(String jobId) {
        Job job = getJobOrThrowIfUnknown(clusterService.state(), jobId);
        if (job.getSchedulerConfig() == null) {
            return Optional.empty();
        }

        Allocation allocation = getAllocation(clusterService.state(), jobId);
        return Optional.ofNullable(allocation.getSchedulerState());
    }

    public void updateSchedulerStatus(UpdateJobSchedulerStatusAction.Request request,
                                      ActionListener<UpdateJobSchedulerStatusAction.Response> actionListener) {
        String jobId = request.getJobId();
        JobSchedulerStatus newStatus = request.getSchedulerStatus();
        clusterService.submitStateUpdateTask("update-scheduler-status-job-" + jobId,
                new AckedClusterStateUpdateTask<UpdateJobSchedulerStatusAction.Response>(request, actionListener) {

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerUpdateSchedulerState(currentState, jobId, newStatus, null, null);
            }

            @Override
            protected UpdateJobSchedulerStatusAction.Response newResponse(boolean acknowledged) {
                return new UpdateJobSchedulerStatusAction.Response(acknowledged);
            }
        });
    }

    private ClusterState innerUpdateSchedulerState(ClusterState currentState, String jobId, JobSchedulerStatus status,
                                                   Long startTime, Long endTime) {
        Job job = getJobOrThrowIfUnknown(currentState, jobId);
        checkJobIsScheduled(job);

        Allocation allocation = getAllocation(currentState, jobId);
        if (allocation.getSchedulerState() == null && status != JobSchedulerStatus.STARTING) {
            throw new IllegalArgumentException("Can't change status to [" + status + "], because job's [" + jobId +
                    "] scheduler never started");
        }

        SchedulerState existingState = allocation.getSchedulerState();
        if (existingState != null) {
            if (startTime == null) {
                startTime = existingState.getStartTimeMillis();
            }
            if (endTime == null) {
                endTime = existingState.getEndTimeMillis();
            }
        }

        existingState = new SchedulerState(status, startTime, endTime);
        Allocation.Builder builder = new Allocation.Builder(allocation);
        builder.setSchedulerState(existingState);
        return innerUpdateAllocation(builder.build(), currentState);
    }

    private Allocation getAllocation(ClusterState state, String jobId) {
        PrelertMetadata prelertMetadata = state.metaData().custom(PrelertMetadata.TYPE);
        Allocation allocation = prelertMetadata.getAllocations().get(jobId);
        if (allocation == null) {
            throw new ResourceNotFoundException("No allocation found for job with id [" + jobId + "]");
        }
        return allocation;
    }

    private ClusterState innerUpdateAllocation(Allocation newAllocation, ClusterState currentState) {
        PrelertMetadata currentPrelertMetadata = currentState.metaData().custom(PrelertMetadata.TYPE);
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        builder.updateAllocation(newAllocation.getJobId(), newAllocation);
        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData()).putCustom(PrelertMetadata.TYPE, builder.build()).build());
        return newState.build();
    }

    public Auditor audit(String jobId) {
        return jobProvider.audit(jobId);
    }

    public Auditor systemAudit() {
        return jobProvider.audit("");
    }

    public void revertSnapshot(RevertModelSnapshotAction.Request request, ActionListener<RevertModelSnapshotAction.Response> actionListener,
            ModelSnapshot modelSnapshot) {

        clusterService.submitStateUpdateTask("revert-snapshot-" + request.getJobId(),
                new AckedClusterStateUpdateTask<RevertModelSnapshotAction.Response>(request, actionListener) {

            @Override
            protected RevertModelSnapshotAction.Response newResponse(boolean acknowledged) {
                RevertModelSnapshotAction.Response response;

                if (acknowledged) {
                    response = new RevertModelSnapshotAction.Response(modelSnapshot);

                    // NORELEASE: This is not the place the audit log
                    // (indexes a document), because this method is
                    // executed on the cluster state update task thread
                    // and any action performed on that thread should be
                    // quick. (so no indexing documents)
                    // audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_REVERTED,
                    // modelSnapshot.getDescription()));

                } else {
                    response = new RevertModelSnapshotAction.Response();
                }
                return response;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                Job job = getJobOrThrowIfUnknown(currentState, request.getJobId());
                Job.Builder builder = new Job.Builder(job);
                builder.setModelSnapshotId(modelSnapshot.getSnapshotId());
                if (request.getDeleteInterveningResults()) {
                    builder.setIgnoreDowntime(IgnoreDowntime.NEVER);
                    DataCounts counts = jobProvider.dataCounts(request.getJobId());
                    counts.setLatestRecordTimeStamp(modelSnapshot.getLatestRecordTimeStamp());
                    // NORELEASE This update should be async. See #127
                    jobDataCountsPersister.persistDataCounts(request.getJobId(), counts);

                } else {
                    builder.setIgnoreDowntime(IgnoreDowntime.ONCE);
                }

                return innerPutJob(builder.build(), true, currentState);
            }
        });
    }

    public void pauseJob(PauseJobAction.Request request, ActionListener<PauseJobAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("pause-job-" + request.getJobId(),
                new AckedClusterStateUpdateTask<PauseJobAction.Response>(request, actionListener) {

                    @Override
                    protected PauseJobAction.Response newResponse(boolean acknowledged) {
                        return new PauseJobAction.Response(acknowledged);
                    }

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        Job job = getJobOrThrowIfUnknown(currentState, request.getJobId());
                        Allocation allocation = getAllocation(currentState, job.getId());
                        checkJobIsNotScheduled(job);
                        if (!allocation.getStatus().isAnyOf(JobStatus.RUNNING, JobStatus.CLOSED)) {
                            throw ExceptionsHelper.conflictStatusException(
                                    Messages.getMessage(Messages.JOB_CANNOT_PAUSE, job.getId(), allocation.getStatus()));
                        }

                        ClusterState newState = innerSetJobStatus(job.getId(), JobStatus.PAUSING, currentState);
                        Job.Builder jobBuilder = new Job.Builder(job);
                        jobBuilder.setIgnoreDowntime(IgnoreDowntime.ONCE);
                        return innerPutJob(jobBuilder.build(), true, newState);
                    }
                });
    }

    public void resumeJob(ResumeJobAction.Request request, ActionListener<ResumeJobAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("resume-job-" + request.getJobId(),
                new AckedClusterStateUpdateTask<ResumeJobAction.Response>(request, actionListener) {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                getJobOrThrowIfUnknown(request.getJobId());
                Allocation allocation = getJobAllocation(request.getJobId());
                if (allocation.getStatus() != JobStatus.PAUSED) {
                    throw ExceptionsHelper.conflictStatusException(
                            Messages.getMessage(Messages.JOB_CANNOT_RESUME, request.getJobId(), allocation.getStatus()));
                }
                Allocation.Builder builder = new Allocation.Builder(allocation);
                builder.setStatus(JobStatus.CLOSED);
                return innerUpdateAllocation(builder.build(), currentState);
            }

            @Override
            protected ResumeJobAction.Response newResponse(boolean acknowledged) {
                return new ResumeJobAction.Response(acknowledged);
            }
        });
    }

    public JobStatus getJobStatus(String jobId) {
        return getJobAllocation(jobId).getStatus();
    }

    public void setJobStatus(UpdateJobStatusAction.Request request, ActionListener<UpdateJobStatusAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("set-job-status-" + request.getStatus() + "-" + request.getJobId(),
                new AckedClusterStateUpdateTask<UpdateJobStatusAction.Response>(request, actionListener) {

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        return innerSetJobStatus(request.getJobId(), request.getStatus(), currentState);
                    }

                    @Override
                    protected UpdateJobStatusAction.Response newResponse(boolean acknowledged) {
                        return new UpdateJobStatusAction.Response(acknowledged);
                    }
                });
    }

    /**
     * Update a persisted model snapshot metadata document to match the
     * argument supplied.
     *
     * @param jobId                 the job id
     * @param modelSnapshot         the updated model snapshot object to be stored
     * @param restoreModelSizeStats should the model size stats in this
     *                              snapshot be made the current ones for this job?
     */
    public void updateModelSnapshot(String jobId, ModelSnapshot modelSnapshot, boolean restoreModelSizeStats) {
        // For Elasticsearch the update can be done in exactly the same way as
        // the original persist
        jobResultsPersister.persistModelSnapshot(modelSnapshot);
        if (restoreModelSizeStats) {
            if (modelSnapshot.getModelSizeStats() != null) {
                jobResultsPersister.persistModelSizeStats(modelSnapshot.getModelSizeStats());
            }
            if (modelSnapshot.getQuantiles() != null) {
                jobResultsPersister.persistQuantiles(modelSnapshot.getQuantiles());
            }
        }
        // Commit so that when the REST API call that triggered the update
        // returns the updated document is searchable
        jobResultsPersister.commitWrites(jobId);
    }

    private ClusterState innerSetJobStatus(String jobId, JobStatus newStatus, ClusterState currentState) {
        Allocation allocation = getJobAllocation(jobId);
        Allocation.Builder builder = new Allocation.Builder(allocation);
        builder.setStatus(newStatus);
        return innerUpdateAllocation(builder.build(), currentState);
    }

    private void checkJobIsNotScheduled(Job job) {
        if (job.getSchedulerConfig() != null) {
            throw ExceptionsHelper.conflictStatusException(Messages.getMessage(Messages.REST_ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));
        }
    }
}
