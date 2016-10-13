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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.job.DeleteJobAction;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.JobIdAlreadyExistsException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.logs.JobLogs;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;
import org.elasticsearch.xpack.prelert.job.metadata.Job;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;

import java.io.IOException;
import java.util.*;
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

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY;

    private static final int LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS = 1000;

    private static final String SCHEDULER_AUTORESTART_SETTING = "scheduler.autorestart";
    private static final boolean DEFAULT_SCHEDULER_AUTORESTART = true;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActionGuardian<Action> processActionGuardian;
    private final ActionGuardian<ScheduledAction> schedulerActionGuardian;
    private final JobProvider jobProvider;
    private final JobFactory jobFactory;
    private final ClusterService clusterService;

    /**
     * Create a JobManager
     */
    public JobManager(JobProvider jobProvider,
                      ClusterService clusterService, ActionGuardian<Action> processActionGuardian,
                      ActionGuardian<ScheduledAction> schedulerActionGuardian) {
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.clusterService = clusterService;
        this.processActionGuardian = Objects.requireNonNull(processActionGuardian);
        this.schedulerActionGuardian = Objects.requireNonNull(schedulerActionGuardian);

        jobFactory = new JobFactory();
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
     * or throws {@link UnknownJobException}
     *
     * @param jobId
     * @return the {@code JobDetails} if a job with the given {@code jobId} exists
     * @throws UnknownJobException if there is no job with matching the given {@code jobId}
     */
    public JobDetails getJobOrThrowIfUnknown(String jobId) throws UnknownJobException {
        Optional<JobDetails> job = jobProvider.getJobDetails(jobId);
        if (!job.isPresent()) {
            throw new UnknownJobException(jobId);
        }
        return job.get();
    }

    /**
     * Stores a job in the cluster state
     */
    public void putJob(PutJobAction.Request request, ActionListener<PutJobAction.Response> actionListener) throws JobIdAlreadyExistsException {
        JobConfiguration jobConfiguration;
        try {
            jobConfiguration = objectMapper.readValue(request.getJobConfiguration().toBytesRef().bytes, JobConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JobDetails jobDetails = jobFactory.create(jobConfiguration);
        jobProvider.createJob(jobDetails);
        clusterService.submitStateUpdateTask("put-job-" + jobDetails.getId(), new AckedClusterStateUpdateTask<PutJobAction.Response>(request, actionListener) {

            @Override
            protected PutJobAction.Response newResponse(boolean acknowledged) {
                // NORELEASE: This is not the place the audit log (indexes a document), because this method is executed on the cluster state update task thread
                // and any action performed on that thread should be quick. (so no indexing documents)
                // audit(jobDetails.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

                // Also I wonder if we need to audit log infra structure in prelert as when we merge into xpack
                // we can use its audit trailing. See: https://github.com/elastic/prelert-legacy/issues/48
                try {
                    return new PutJobAction.Response(jobDetails, objectMapper);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
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


    public void updateCustomSettings(String jobId, Map<String, Object> customSettings)
            throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.CUSTOM_SETTINGS, customSettings);
    }

    private void updateJobTopLevelKeyValue(String jobId, String key, Object value)
            throws UnknownJobException {
        Map<String, Object> update = new HashMap<>();
        update.put(key, value);
        jobProvider.updateJob(jobId, update);
    }

    /**
     * Updates a job with new {@code AnalysisLimits}.
     *
     * @param jobId     the job id
     * @param newLimits the new limits
     * @throws UnknownJobException if the job if unknown
     */
    public void setAnalysisLimits(String jobId, AnalysisLimits newLimits) throws UnknownJobException {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.ANALYSIS_LIMITS, newLimits.toMap());
        jobProvider.updateJob(jobId, update);
    }

    /**
     * Set the job's description.
     * If the description cannot be set an exception is thrown.
     *
     * @param jobId
     * @param description
     * @throws UnknownJobException
     */
    public void setDescription(String jobId, String description)
            throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.DESCRIPTION, description);
    }

    public void setModelDebugConfig(String jobId, ModelDebugConfig modelDebugConfig)
            throws UnknownJobException {
        Map<String, Object> update = new HashMap<>();
        if (modelDebugConfig != null) {
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put(ModelDebugConfig.WRITE_TO, modelDebugConfig.getWriteTo());
            objectMap.put(ModelDebugConfig.BOUNDS_PERCENTILE, modelDebugConfig.getBoundsPercentile());
            objectMap.put(ModelDebugConfig.TERMS, modelDebugConfig.getTerms());
            update.put(JobDetails.MODEL_DEBUG_CONFIG, objectMap);
        } else {
            update.put(JobDetails.MODEL_DEBUG_CONFIG, null);
        }
        jobProvider.updateJob(jobId, update);
    }

    public void setRenormalizationWindowDays(String jobId, Long renormalizationWindowDays) throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.RENORMALIZATION_WINDOW_DAYS, renormalizationWindowDays);
    }

    public void setModelSnapshotRetentionDays(String jobId, Long retentionDays) throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS, retentionDays);
    }

    public void setResultsRetentionDays(String jobId, Long retentionDays) throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.RESULTS_RETENTION_DAYS, retentionDays);
    }

    public void setBackgroundPersistInterval(String jobId, Long backgroundPersistInterval)
            throws UnknownJobException {
        updateJobTopLevelKeyValue(jobId, JobDetails.BACKGROUND_PERSIST_INTERVAL, backgroundPersistInterval);
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
        // NORELEASE: Should also handle scheduled jobs

        try (ActionGuardian<Action>.ActionTicket actionTicket = processActionGuardian.tryAcquiringAction(jobId, Action.DELETING)) {
            // NORELEASE: Index/Logs deletion should be done after job is removed from state in a forked thread
            if (jobProvider.deleteJob(jobId)) {
                new JobLogs().deleteLogs(jobId);
                clusterService.submitStateUpdateTask("delete-job-" + jobId, new AckedClusterStateUpdateTask<DeleteJobAction.Response>(request, actionListener) {

                    @Override
                    protected DeleteJobAction.Response newResponse(boolean acknowledged) {
                        // NORELEASE: This is not the place the audit log (indexes a document), because this method is executed on the cluster state update task thread
                        // and any action performed on that thread should be quick. (so no indexing documents)
                        // audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));

                        // Also I wonder if we need to audit log infra structure in prelert as when we merge into xpack
                        // we can use its audit trailing. See: https://github.com/elastic/prelert-legacy/issues/48
                        return new DeleteJobAction.Response(acknowledged);
                    }

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        return removeJobFromClusterState(jobId, currentState);
                    }
                });
            }
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


    public Auditor audit(String jobId) {
        return jobProvider.audit(jobId);
    }

    public Auditor systemAudit() {
        return jobProvider.audit("");
    }
}
