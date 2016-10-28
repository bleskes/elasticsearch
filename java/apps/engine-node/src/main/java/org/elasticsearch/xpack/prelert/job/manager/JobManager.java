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
package org.elasticsearch.xpack.prelert.job.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction;
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.StopJobSchedulerAction;
import org.elasticsearch.xpack.prelert.action.UpdateJobAction;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.JobIdAlreadyExistsException;
import org.elasticsearch.xpack.prelert.job.logs.JobLogs;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.metadata.Job;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.update.JobUpdater;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Allows interactions with jobs. The managed interactions include:
 * <p>
 * <ul>
 * <li>creation
 * <li>deletion
 * <li>flushing
 * <li>updating
 * <li>sending of data
 * <li>fetching jobs and results
 * <li>starting/stopping of scheduled jobs
 * </ul
 */
public class JobManager {

    private static final Logger LOGGER = Loggers.getLogger(JobManager.class);

    /**
     * Field name in which to store the API version in the usage info
     */
    public static final String APP_VER_FIELDNAME = "appVer";

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY.getPreferredName();

    private static final int LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS = 1000;

    private static final String SCHEDULER_AUTORESTART_SETTING = "scheduler.autorestart";
    private static final boolean DEFAULT_SCHEDULER_AUTORESTART = true;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActionGuardian<Action> processActionGuardian;
    private final JobProvider jobProvider;
    private final ClusterService clusterService;

    /**
     * Create a JobManager
     */
    public JobManager(JobProvider jobProvider,
            ClusterService clusterService, ActionGuardian<Action> processActionGuardian) {
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.clusterService = clusterService;
        this.processActionGuardian = Objects.requireNonNull(processActionGuardian);
    }

    /**
     * Get the details of the specific job wrapped in a <code>Optional</code>
     *
     * @param jobId
     * @return An {@code Optional} containing the {@code JobDetails} if a job with the given
     * {@code jobId} exists, or an empty {@code Optional} otherwise
     */
    public Optional<JobDetails> getJob(String jobId, ClusterState clusterState) {
        PrelertMetadata prelertMetadata = clusterState.getMetaData().custom(PrelertMetadata.TYPE);
        if (prelertMetadata == null) {
            return Optional.empty();
        }

        Job job = prelertMetadata.getJobs().get(jobId);
        if (job == null) {
            return Optional.empty();
        }

        return Optional.of(job.getJobDetails());
    }

    /**
     * Get details of all Jobs.
     *
     * @param skip Skip the first N Jobs. This parameter is for paging
     *             results if not required set to 0.
     * @param take Take only this number of Jobs
     * @return A query page object with hitCount set to the total number
     * of jobs not the only the number returned here as determined by the
     * <code>take</code>
     * parameter.
     */
    public QueryPage<JobDetails> getJobs(int skip, int take, ClusterState clusterState) {
        PrelertMetadata prelertMetadata = clusterState.getMetaData().custom(PrelertMetadata.TYPE);
        if (prelertMetadata == null) {
            return new QueryPage<>(Collections.emptyList(), 0);
        }

        List<JobDetails> jobs = prelertMetadata.getJobs().entrySet().stream()
                .skip(skip)
                .limit(take)
                .map(Map.Entry::getValue)
                .map(Job::getJobDetails)
                .collect(Collectors.toList());
        return new QueryPage<>(jobs, prelertMetadata.getJobs().size());
    }

    /**
     * Returns the non-null {@code JobDetails} object for the given {@code jobId}
     * or throws {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     * @return the {@code JobDetails} if a job with the given {@code jobId} exists
     * @throws org.elasticsearch.ResourceNotFoundException if there is no job with matching the given {@code jobId}
     */
    public JobDetails getJobOrThrowIfUnknown(String jobId) {
        return getJobOrThrowIfUnknown(clusterService.state(), jobId);
    }

    /**
     * Returns the non-null {@code JobDetails} object for the given {@code jobId}
     * or throws {@link org.elasticsearch.ResourceNotFoundException}
     *
     * @param jobId
     * @return the {@code JobDetails} if a job with the given {@code jobId} exists
     * @throws org.elasticsearch.ResourceNotFoundException if there is no job with matching the given {@code jobId}
     */
    public JobDetails getJobOrThrowIfUnknown(ClusterState clusterState, String jobId) {
        PrelertMetadata prelertMetadata = clusterState.metaData().custom(PrelertMetadata.TYPE);
        if (prelertMetadata == null) {
            throw ExceptionsHelper.missingException(jobId);
        }
        Job job = prelertMetadata.getJobs().get(jobId);
        if (job == null) {
            throw ExceptionsHelper.missingException(jobId);
        }
        return job.getJobDetails();
    }

    /**
     * Stores a job in the cluster state
     */
    public void putJob(PutJobAction.Request request, ActionListener<PutJobAction.Response> actionListener) throws JobIdAlreadyExistsException {
        JobConfiguration jobConfiguration = request.getJobConfiguration();
        JobConfigurationVerifier.verify(jobConfiguration);
        // TODO: Remove once all validation happens in JobConfiguration#build() method:
        JobDetails jobDetails = jobConfiguration.build();
        ActionListener<Boolean> delegateListener = new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean acked) {
                try {
                    jobProvider.createJob(jobDetails, new ActionListener<Boolean>() {
                        @Override
                        public void onResponse(Boolean aBoolean) {
                            // NORELEASE: make auditing async too (we can't do blocking stuff here):
                            // audit(jobDetails.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

                            // Also I wonder if we need to audit log infra structure in prelert as when we merge into xpack
                            // we can use its audit trailing. See: https://github.com/elastic/prelert-legacy/issues/48
                            actionListener.onResponse(new PutJobAction.Response(jobDetails));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            actionListener.onFailure(e);

                        }
                    });
                } catch (JobIdAlreadyExistsException e) {
                    actionListener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                actionListener.onFailure(e);
            }
        };
        clusterService.submitStateUpdateTask("put-job-" + jobDetails.getId(), new AckedClusterStateUpdateTask<Boolean>(request, delegateListener) {

            @Override
            protected Boolean newResponse(boolean acknowledged) {
                return acknowledged;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerPutJob(jobDetails, request.isOverwrite(), currentState);
            }

        });
    }

    ClusterState innerPutJob(JobDetails jobDetails, boolean overwrite, ClusterState currentState) {
        PrelertMetadata currentPrelertMetadata = currentState.metaData().custom(PrelertMetadata.TYPE);
        PrelertMetadata.Builder builder;
        if (currentPrelertMetadata != null) {
            builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        } else {
            builder = new PrelertMetadata.Builder();
        }

        builder.putJob(new Job(jobDetails), overwrite);

        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData())
                .putCustom(PrelertMetadata.TYPE, builder.build())
                .build());
        return newState.build();
    }

    /**
     * Deletes a job.
     *
     * The clean-up involves:
     *   <ul>
     *       <li>Deleting the index containing job results</li>
     *       <li>Deleting the job logs</li>
     *       <li>Removing the job from the cluster state</li>
     *   </ul>
     *
     * @param request the delete job request
     * @param actionListener the action listener
     * @throws JobException If the job could not be deleted
     */
    public void deleteJob(DeleteJobAction.Request request, ActionListener<DeleteJobAction.Response> actionListener) throws JobException {
        String jobId = request.getJobId();

        LOGGER.debug("Deleting job '" + jobId + "'");

        // NORELEASE: Should also delete the running process

        try (ActionGuardian<Action>.ActionTicket actionTicket = processActionGuardian.tryAcquiringAction(jobId, Action.DELETING)) {
            checkJobHasNoRunningScheduler(jobId);

            ActionListener<Boolean> delegateListener = new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean aBoolean) {
                    jobProvider.deleteJob(request.getJobId(), new ActionListener<Boolean>() {
                        @Override
                        public void onResponse(Boolean aBoolean) {

                            try {
                                new JobLogs().deleteLogs(jobId);
                                // NORELEASE: This is not the place the audit log (indexes a document), because this method is executed on
                                // the cluster state update task thread  and any action performed on that thread should be quick.
                                // audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));

                                // Also I wonder if we need to audit log infra structure in prelert as when we merge into xpack
                                // we can use its audit trailing. See: https://github.com/elastic/prelert-legacy/issues/48
                            } catch (JobException e) {
                                actionListener.onFailure(e);
                            }
                            actionListener.onResponse(new DeleteJobAction.Response(aBoolean));
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

            clusterService.submitStateUpdateTask("delete-job-" + jobId, new AckedClusterStateUpdateTask<Boolean>(request, delegateListener) {

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
    }

    private void checkJobHasNoRunningScheduler(String jobId) {
        JobDetails job = getJobOrThrowIfUnknown(jobId);
        SchedulerState schedulerState = job.getSchedulerState();
        if (schedulerState != null && schedulerState.getStatus() != JobSchedulerStatus.STOPPED) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CANNOT_DELETE_WHILE_SCHEDULER_RUNS, jobId),
                    ErrorCodes.CANNOT_DELETE_JOB_SCHEDULER);
        }
    }

    ClusterState removeJobFromClusterState(String jobId, ClusterState currentState) {
        PrelertMetadata currentPrelertMetadata = currentState.metaData().custom(PrelertMetadata.TYPE);
        if (currentPrelertMetadata == null) {
            return currentState;
        }

        PrelertMetadata.Builder builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        builder.removeJob(jobId);

        ClusterState.Builder newState = ClusterState.builder(currentState);
        newState.metaData(MetaData.builder(currentState.getMetaData())
                .putCustom(PrelertMetadata.TYPE, builder.build())
                .build());
        return newState.build();
    }

    public void updateJob(UpdateJobAction.Request request, ActionListener<UpdateJobAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("update-job-" + request.getJobId(), new AckedClusterStateUpdateTask<UpdateJobAction.Response>(request, actionListener) {

            @Override
            protected UpdateJobAction.Response newResponse(boolean acknowledged) {
                return new UpdateJobAction.Response(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                JobDetails jobDetails = getJobOrThrowIfUnknown(currentState, request.getJobId());
                new JobUpdater(jobDetails).update(request.getUpdateJson());
                return innerPutJob(jobDetails, true, currentState);
            }
        });
    }

    public void startJobScheduler(StartJobSchedulerAction.Request request, ActionListener<StartJobSchedulerAction.Response> actionListener) {
        clusterService.submitStateUpdateTask("start-scheduler-job-" + request.getJobId(),
                new AckedClusterStateUpdateTask<StartJobSchedulerAction.Response>(request, actionListener) {

            @Override
            protected StartJobSchedulerAction.Response newResponse(boolean acknowledged) {
                return new StartJobSchedulerAction.Response(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerUpdateSchedulerState(currentState, request.getJobId(), oldState -> request.getSchedulerState());
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
                return innerUpdateSchedulerState(currentState, request.getJobId(), oldState -> new SchedulerState(
                        JobSchedulerStatus.STOPPING, oldState.getStartTimeMillis(), oldState.getEndTimeMillis()));
            }
        });
    }

    private void checkJobIsScheduled(JobDetails job) {
        if (job.getSchedulerConfig() == null) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_NO_SUCH_SCHEDULED_JOB, job.getId()),
                    ErrorCodes.NO_SUCH_SCHEDULED_JOB);
        }
    }

    private void validateSchedulerStateTransition(JobDetails jobDetails, SchedulerState newState) {
        SchedulerState currentSchedulerState = jobDetails.getSchedulerState();
        JobSchedulerStatus currentSchedulerStatus = currentSchedulerState == null ? JobSchedulerStatus.STOPPED
                : currentSchedulerState.getStatus();
        JobSchedulerStatus newSchedulerStatus = newState.getStatus();
        switch (newSchedulerStatus) {
        case STARTED:
            if (currentSchedulerStatus != JobSchedulerStatus.STOPPED) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_START,
                        jobDetails.getId(), jobDetails.getSchedulerState().getStatus()), ErrorCodes.CANNOT_START_JOB_SCHEDULER);
            }
            break;
        case STOPPING:
            if (currentSchedulerStatus != JobSchedulerStatus.STARTED) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE,
                        jobDetails.getId(), jobDetails.getSchedulerState().getStatus()), ErrorCodes.CANNOT_STOP_JOB_SCHEDULER);
            }
            break;
        case STOPPED:
            if (currentSchedulerStatus != JobSchedulerStatus.STOPPING) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE,
                        jobDetails.getId(), jobDetails.getSchedulerState().getStatus()), ErrorCodes.CANNOT_STOP_JOB_SCHEDULER);
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid requested job scheduler status: " + newSchedulerStatus);
        }
    }

    public void updateSchedulerStatus(String jobId, JobSchedulerStatus newSchedulerStatus) {
        clusterService.submitStateUpdateTask("udpate-scheduler-status-job-" + jobId,
                new ClusterStateUpdateTask() {

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return innerUpdateSchedulerState(currentState, jobId, oldSchedulerState -> new SchedulerState(
                        newSchedulerStatus, oldSchedulerState.getStartTimeMillis(), oldSchedulerState.getEndTimeMillis()));
            }

            @Override
            public void onFailure(String source, Exception e) {
                LOGGER.error("Error updating scheduler status: source=[" + source + "], status=[" + newSchedulerStatus + "]", e);
            }
        });
    }

    private ClusterState innerUpdateSchedulerState(ClusterState currentState, String jobId,
            Function<SchedulerState, SchedulerState> stateUpdater) {
        JobDetails jobDetails = getJobOrThrowIfUnknown(currentState, jobId);
        checkJobIsScheduled(jobDetails);
        SchedulerState oldState = jobDetails.getSchedulerState();
        SchedulerState newState = stateUpdater.apply(oldState);
        validateSchedulerStateTransition(jobDetails, newState);
        jobDetails.setSchedulerState(newState);
        return innerPutJob(jobDetails, true, currentState);
    }

    public Auditor audit(String jobId) {
        return jobProvider.audit(jobId);
    }

    public Auditor systemAudit() {
        return jobProvider.audit("");
    }

    public void revertSnapshot(RevertModelSnapshotAction.Request request,
            ActionListener<RevertModelSnapshotAction.Response> actionListener,
            ModelSnapshot modelSnapshot) {

        clusterService.submitStateUpdateTask("revert-snapshot-" + request.getJobId(),
                new AckedClusterStateUpdateTask<RevertModelSnapshotAction.Response>(request, actionListener) {

            @Override
            protected RevertModelSnapshotAction.Response newResponse(boolean acknowledged) {
                RevertModelSnapshotAction.Response response;

                if (acknowledged) {
                            response = new RevertModelSnapshotAction.Response(modelSnapshot);

                    // NORELEASE: This is not the place the audit log (indexes a document), because this method is executed on the cluster state update task thread
                    // and any action performed on that thread should be quick. (so no indexing documents)
                    //audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_REVERTED,
                    //        modelSnapshot.getDescription()));

                } else {
                    response = new RevertModelSnapshotAction.Response();
                }
                return response;
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                JobDetails jobDetails = getJobOrThrowIfUnknown(currentState, request.getJobId());
                jobDetails.setModelSnapshotId(modelSnapshot.getSnapshotId());
                if (request.getDeleteInterveningResults()) {
                    jobDetails.setIgnoreDowntime(IgnoreDowntime.NEVER);

                    Date latestRecordTime = modelSnapshot.getLatestResultTimeStamp();
                    LOGGER.info("Resetting latest record time to '" +  latestRecordTime + "'");
                    jobDetails.setLastDataTime(latestRecordTime);
                    DataCounts counts = jobDetails.getCounts();
                    counts.setLatestRecordTimeStamp(latestRecordTime);
                    jobDetails.setCounts(counts);
                } else {
                    jobDetails.setIgnoreDowntime(IgnoreDowntime.ONCE);
                }

                return innerPutJob(jobDetails, true, currentState);
            }
        });
    }
}
