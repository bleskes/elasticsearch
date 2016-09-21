package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.exceptions.*;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.ActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;

import org.apache.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.DataStoreException;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;

import java.util.*;

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
    private static final Logger LOGGER = Logger.getLogger(JobManager.class);

    /**
     * Field name in which to store the API version in the usage info
     */
    public static final String APP_VER_FIELDNAME = "appVer";

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY;

    private static final int LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS = 1000;

    private static final String SCHEDULER_AUTORESTART_SETTING = "scheduler.autorestart";
    private static final boolean DEFAULT_SCHEDULER_AUTORESTART = true;

    private final ActionGuardian<Action> processActionGuardian;
    private final ActionGuardian<ScheduledAction> schedulerActionGuardian;
    private final JobProvider jobProvider;
    private final JobFactory jobFactory;

    /**
     * Create a JobManager
     */
    public JobManager(JobProvider jobProvider,
                      ActionGuardian<Action> processActionGuardian,
                      ActionGuardian<ScheduledAction> schedulerActionGuardian) {
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.processActionGuardian = Objects.requireNonNull(processActionGuardian);
        this.schedulerActionGuardian = Objects.requireNonNull(schedulerActionGuardian);

        jobFactory = new JobFactory();
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
     * Create a new job from the configuration object and insert into the
     * document store. The details of the newly created job are returned.
     *
     * @param jobConfig
     * @param overwrite If another job with the same name exists, should
     *                  it be overwritten?
     * @return The new job or <code>null</code> if an exception occurs.
     * @throws JobConfigurationException    If the license is violated
     * @throws JobIdAlreadyExistsException  If the job ID is already taken
     * @throws DataStoreException           Possible only if overwriting
     * @throws JobInUseException            Possible only if overwriting
     */
    public JobDetails createJob(JobConfiguration jobConfig, boolean overwrite)
            throws LicenseViolationException, JobConfigurationException, JobIdAlreadyExistsException,
            DataStoreException, JobInUseException {

        JobDetails jobDetails = jobFactory.create(jobConfig);
        String jobId = jobDetails.getId();
        if (!jobProvider.jobIdIsUnique(jobId)) {
            try {
                // A job with the desired ID already exists - try to delete it
                // if we've been told to overwrite
                if (overwrite == false || deleteJob(jobId) == false) {
                    throw new JobIdAlreadyExistsException(jobId);
                }
                LOGGER.debug("Overwriting job '" + jobId + "'");
            } catch (UnknownJobException e) {
                // This implies another request to delete the job has executed
                // at the same time as the overwrite request.  In this case we
                // should be good to continue, as long as the createJob method
                // in org.elasticsearch.xpack.prelert.rs.resources.Jobs only allows one job creation
                // at a time (which it did at the time of writing this comment).
                LOGGER.warn("Independent deletion of job '" + jobId +
                        "' occurred whilst overwriting it");
            }
        }

        jobProvider.createJob(jobDetails);
        audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));
        return jobDetails;
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
     * Stop the associated process and remove it from the Process
     * Manager then delete the job related documents from the
     * database.
     *
     * @param jobId
     * @return
     * @throws UnknownJobException          If the jobId is not recognised
     * @throws DataStoreException           If there is an error deleting the job
     * @throws JobInUseException            If the job cannot be deleted because the
     *                                      native process is in use.
     */
    public boolean deleteJob(String jobId) throws UnknownJobException, DataStoreException {
        LOGGER.debug("Deleting job '" + jobId + "'");

        boolean success = jobProvider.deleteJob(jobId);
        if (success) {
            audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));
        }
        return success;
    }


    public Auditor audit(String jobId) {
        return jobProvider.audit(jobId);
    }

    public Auditor systemAudit() {
        return jobProvider.audit("");
    }
}
