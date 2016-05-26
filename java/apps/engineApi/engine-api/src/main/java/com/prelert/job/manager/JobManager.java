/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.manager;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.prelert.app.Shutdownable;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataCounts;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.NoSuchModelSnapshotException;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.audit.Auditor;
import com.prelert.job.config.DefaultFrequency;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.data.extraction.DataExtractorFactory;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;
import com.prelert.job.manager.actions.ScheduledAction;
import com.prelert.job.messages.Messages;
import com.prelert.job.password.PasswordManager;
import com.prelert.job.persistence.BatchedDocumentsIterator;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobDataDeleterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.OldDataRemover;
import com.prelert.job.persistence.none.NoneJobDataPersister;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.process.exceptions.ClosedJobException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.writer.CsvRecordWriter;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
import com.prelert.job.scheduler.CannotUpdateSchedulerException;
import com.prelert.job.scheduler.DataProcessor;
import com.prelert.job.scheduler.JobScheduler;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.none.NoneStatusReporter;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.settings.PrelertSettings;

/**
 * Allows interactions with jobs. The managed interactions include:
 *
 * <ul>
 *   <li>creation
 *   <li>deletion
 *   <li>flushing
 *   <li>updating
 *   <li>sending of data
 *   <li>fetching jobs and results
 *   <li>starting/stopping of scheduled jobs
 * </ul
 */
public class JobManager implements DataProcessor, Shutdownable, Feature
{
    private static final Logger LOGGER = Logger.getLogger(JobManager.class);

    /**
     * Field name in which to store the API version in the usage info
     */
    public static final String APP_VER_FIELDNAME = "appVer";

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY;

    private static final int LAST_DATA_TIME_CACHE_SIZE = 1000;
    private static final int LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS = 1000;

    private static final String SCHEDULER_AUTORESTART_SETTING = "scheduler.autorestart";
    private static final boolean DEFAULT_SCHEDULER_AUTORESTART = true;

    private final ActionGuardian<Action> m_ProcessActionGuardian;
    private final ActionGuardian<ScheduledAction> m_SchedulerActionGuardian;
    private final JobProvider m_JobProvider;
    private final ProcessManager m_ProcessManager;
    private final DataExtractorFactory m_DataExtractorFactory;
    private final JobLoggerFactory m_JobLoggerFactory;
    private final PasswordManager m_PasswordManager;
    private final JobAutoCloser m_JobAutoCloser;

    private final JobFactory m_JobFactory;
    private final LicenceChecker m_LicenceChecker;
    private final JobDataDeleterFactory m_JobDataDeleterFactory;

    /**
     * A map holding schedulers by jobId.
     * The map is concurrent to ensure correct visibility as it is expected
     * to be accessed by multiple threads.
     */
    private final ConcurrentHashMap<String, JobScheduler> m_ScheduledJobs;

    /**
     * A cache that stores the epoch in seconds of the last
     * time data was sent to a job. The cache is used to avoid
     * updating a job with the same last data time multiple times,
     * which in turn avoids any version conflict errors from
     * the storage.
     */
    private final Cache<String, Long> m_LastDataTimePerJobCache;

    /**
     * Create a JobManager
     *
     * @param jobDetailsProvider
     */
    public JobManager(JobProvider jobProvider, ProcessManager processManager,
            DataExtractorFactory dataExtractorFactory, JobLoggerFactory jobLoggerFactory,
            PasswordManager passwordManager, JobDataDeleterFactory jobDataDeleterFactory,
            ActionGuardian<Action> processActionGuardian,
            ActionGuardian<ScheduledAction> schedulerActionGuardian)
    {
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_ProcessManager = Objects.requireNonNull(processManager);
        m_DataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_PasswordManager = Objects.requireNonNull(passwordManager);
        m_JobAutoCloser = new JobAutoCloser(jobId -> closeJob(jobId));
        m_ProcessActionGuardian = Objects.requireNonNull(processActionGuardian);
        m_SchedulerActionGuardian = Objects.requireNonNull(schedulerActionGuardian);

        m_ScheduledJobs = new ConcurrentHashMap<>();
        m_LastDataTimePerJobCache = CacheBuilder.newBuilder()
                .maximumSize(LAST_DATA_TIME_CACHE_SIZE)
                .build();

        // This requires the process manager and data storage connection in
        // order to work, but failure is considered non-fatal
        m_LicenceChecker = new LicenceChecker(
                BackendInfo.fromJson(m_ProcessManager.getInfo(), m_JobProvider, apiVersion()));

        m_JobFactory = new JobFactory();
        m_JobDataDeleterFactory = Objects.requireNonNull(jobDataDeleterFactory);
    }

    /**
     * Required by the Feature interface.
     */
    @Override
    public boolean configure(FeatureContext context)
    {
        return true;
    }

    /**
     * Returns the non-null {@code JobDetails} object for the given {@code jobId}
     * or throws {@link UnknownJobException}
     *
     * @param jobId
     * @return the {@code JobDetails} if a job with the given {@code jobId} exists
     * @throws UnknownJobException if there is no job with matching the given {@code jobId}
     */
    public JobDetails getJobOrThrowIfUnknown(String jobId) throws UnknownJobException
    {
        Optional<JobDetails> job = m_JobProvider.getJobDetails(jobId);
        if (!job.isPresent())
        {
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
     * it be overwritten?
     * @return The new job or <code>null</code> if an exception occurs.
     * @throws TooManyJobsException If the license is violated
     * @throws JobConfigurationException If the license is violated
     * @throws JobIdAlreadyExistsException If the job ID is already taken
     * @throws DataStoreException Possible only if overwriting
     * @throws NativeProcessRunException Possible only if overwriting
     * @throws JobInUseException Possible only if overwriting
     * @throws CannotStopSchedulerException Possible only if overwriting
     */
    public JobDetails createJob(JobConfiguration jobConfig, boolean overwrite)
            throws LicenseViolationException, JobConfigurationException, JobIdAlreadyExistsException,
            DataStoreException, NativeProcessRunException, JobInUseException,
            CannotStopSchedulerException
    {
        JobDetails jobDetails = m_JobFactory.create(securePasswords(jobConfig));
        String jobId = jobDetails.getId();
        if (!m_JobProvider.jobIdIsUnique(jobId))
        {
            try
            {
                // A job with the desired ID already exists - try to delete it
                // if we've been told to overwrite
                if (overwrite == false || deleteJob(jobId) == false)
                {
                    throw new JobIdAlreadyExistsException(jobId);
                }
                LOGGER.debug("Overwriting job '" + jobId + "'");
            }
            catch (UnknownJobException e)
            {
                // This implies another request to delete the job has executed
                // at the same time as the overwrite request.  In this case we
                // should be good to continue, as long as the createJob method
                // in com.prelert.rs.resources.Jobs only allows one job creation
                // at a time (which it did at the time of writing this comment).
                LOGGER.warn("Independent deletion of job '" + jobId +
                        "' occurred whilst overwriting it");
            }
        }

        // Check licence now after any duplicate job conditions have been found
        // throws
        m_LicenceChecker.checkLicenceViolationsOnCreate(jobDetails.getAnalysisConfig(),
                                                    getActiveJobIds().size());

        m_JobProvider.createJob(jobDetails);
        audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

        if (jobDetails.getSchedulerConfig() != null)
        {
            createJobScheduler(jobDetails);
        }

        return jobDetails;
    }

    private JobConfiguration securePasswords(JobConfiguration jobConfig)
            throws JobConfigurationException
    {
        if (jobConfig != null && jobConfig.getSchedulerConfig() != null)
        {
            securePassword(jobConfig.getSchedulerConfig());
        }
        return jobConfig;
    }

    private void securePassword(SchedulerConfig schedulerConfig) throws JobConfigurationException
    {
        try
        {
            m_PasswordManager.secureStorage(schedulerConfig);
        }
        catch (GeneralSecurityException e)
        {
            throw new JobConfigurationException(Messages.getMessage(Messages.JOB_CONFIG_CANNOT_ENCRYPT_PASSWORD),
                    ErrorCodes.ENCRYPTION_FAILURE_ERROR, e);
        }
    }

    private JobScheduler createJobScheduler(JobDetails job)
    {
        Duration bucketSpan = Duration.ofSeconds(job.getAnalysisConfig().getBucketSpan());
        Duration frequency = getFrequencyOrDefault(job);
        Duration queryDelay = Duration.ofSeconds(job.getSchedulerConfig().getQueryDelay());
        JobScheduler jobScheduler = new JobScheduler(job.getId(), bucketSpan, frequency, queryDelay,
                m_DataExtractorFactory.newExtractor(job), this, m_JobProvider,
                m_JobLoggerFactory);
        m_ScheduledJobs.put(job.getId(), jobScheduler);
        return jobScheduler;
    }

    private static Duration getFrequencyOrDefault(JobDetails job)
    {
        Long frequency = job.getSchedulerConfig().getFrequency();
        Long bucketSpan = job.getAnalysisConfig().getBucketSpan();
        return frequency == null ? DefaultFrequency.ofBucketSpan(bucketSpan)
                : Duration.ofSeconds(frequency);
    }

    public void resetLatestRecordTime(String jobId, Date latestRecordTime) throws UnknownJobException
    {
        m_JobLoggerFactory.newLogger(jobId).info("Resetting latest record time to '" +  latestRecordTime + "'");

        JobDetails jb = m_JobProvider.getJobDetails(jobId).get();
        jb.setLastDataTime(latestRecordTime);
        DataCounts counts = jb.getCounts();
        counts.setLatestRecordTimeStamp(latestRecordTime);

        Map<String, Object> update = new HashMap<>();
        Map<String, Object> objectMap = counts.toObjectMap();
        update.put(JobDetails.COUNTS, objectMap);
        m_JobProvider.updateJob(jobId, update);
    }

    public void deleteBucketsAfter(String jobId, Date deleteAfter)
    {
        m_JobLoggerFactory.newLogger(jobId).info("Deleting buckets after '" + deleteAfter + "'");
        OldDataRemover remover = new OldDataRemover(m_JobProvider, m_JobDataDeleterFactory);
        remover.deleteResultsAfter(jobId, deleteAfter.getTime() + 1);
    }

    public ModelSnapshot revertToSnapshot(String jobId, long epochEndMs, String snapshotId,
            String description, boolean dontSkip)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.REVERTING))
        {
            if (m_ProcessManager.jobIsRunning(jobId))
            {
                throw new JobInUseException(Messages.getMessage(Messages.REST_JOB_NOT_CLOSED_REVERT),
                        ErrorCodes.JOB_NOT_CLOSED);
            }
            m_JobLoggerFactory.newLogger(jobId).info("Reverting to snapshot '" + snapshotId +
                    "' for time '" + epochEndMs + "'");

            List<ModelSnapshot> revertCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, epochEndMs, ModelSnapshot.TIMESTAMP, true, snapshotId, description).queryResults();

            if (revertCandidates == null || revertCandidates.isEmpty())
            {
                throw new NoSuchModelSnapshotException(jobId);
            }

            ModelSnapshot modelSnapshot = revertCandidates.get(0);
            // Add 1 to the current time so that if this call immediately follows
            // a job close this restore priority is higher rather than equal
            modelSnapshot.setRestorePriority(System.currentTimeMillis() + 1);

            // In addition to the checked exceptions, this may throw an
            // ElasticsearchException if it fails
            m_JobProvider.updateModelSnapshot(jobId, modelSnapshot, true);

            if (dontSkip)
            {
                updateIgnoreDowntime(jobId, IgnoreDowntime.NEVER);
            }
            else
            {
                updateIgnoreDowntime(jobId, IgnoreDowntime.ONCE);
            }
            audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_REVERTED,
                    modelSnapshot.getDescription()));
            return modelSnapshot;
        }
    }

    public ModelSnapshot updateModelSnapshotDescription(String jobId, String snapshotId,
            String newDescription)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            DescriptionAlreadyUsedException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                        m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
        {
            List<ModelSnapshot> changeCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, 0, null, true, snapshotId, null).queryResults();

            if (changeCandidates == null || changeCandidates.isEmpty())
            {
                throw new NoSuchModelSnapshotException(jobId);
            }

            List<ModelSnapshot> clashCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, 0, null, true, null, newDescription).queryResults();

            if (clashCandidates != null && !clashCandidates.isEmpty())
            {
                throw new DescriptionAlreadyUsedException(jobId, newDescription);
            }

            ModelSnapshot modelSnapshot = changeCandidates.get(0);
            modelSnapshot.setDescription(newDescription);

            // In addition to the checked exceptions, this may throw an
            // ElasticsearchException if it fails
            m_JobProvider.updateModelSnapshot(jobId, modelSnapshot, false);

            return modelSnapshot;
        }
    }

    public ModelSnapshot deleteModelSnapshot(String jobId, String snapshotId)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            CannotDeleteSnapshotException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
        {
            List<ModelSnapshot> highestPriority = m_JobProvider.modelSnapshots(jobId, 0, 1).queryResults();
            if (highestPriority == null || highestPriority.isEmpty())
            {
                throw new NoSuchModelSnapshotException(jobId);
            }

            String highestPriorityId = highestPriority.get(0).getSnapshotId();
            if (highestPriorityId.equals(snapshotId))
            {
                throw new CannotDeleteSnapshotException(jobId, highestPriorityId);
            }

            ModelSnapshot deletedSnapshot = m_JobProvider.deleteModelSnapshot(jobId, snapshotId);

            audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_SNAPSHOT_DELETED,
                    deletedSnapshot.getDescription()));
            return deletedSnapshot;
        }
    }

    public void updateCustomSettings(String jobId, Map<String, Object> customSettings)
            throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.CUSTOM_SETTINGS, customSettings);
    }

    private void updateJobTopLevelKeyValue(String jobId, String key, Object value)
            throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(key, value);
        m_JobProvider.updateJob(jobId, update);
    }

    public void updateIgnoreDowntime(String jobId, IgnoreDowntime ignoreDowntime)
            throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.IGNORE_DOWNTIME, ignoreDowntime);
    }

    /**
     * Updates a job with new {@code AnalysisLimits}.
     * @param jobId the job id
     * @param newLimits the new limits
     * @throws UnknownJobException if the job if unknown
     */
    public void setAnalysisLimits(String jobId, AnalysisLimits newLimits) throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.ANALYSIS_LIMITS, newLimits.toMap());
        m_JobProvider.updateJob(jobId, update);
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
    throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.DESCRIPTION, description);
    }

    public void setModelDebugConfig(String jobId, ModelDebugConfig modelDebugConfig)
            throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        if (modelDebugConfig != null)
        {
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put(ModelDebugConfig.WRITE_TO, modelDebugConfig.getWriteTo());
            objectMap.put(ModelDebugConfig.BOUNDS_PERCENTILE, modelDebugConfig.getBoundsPercentile());
            objectMap.put(ModelDebugConfig.TERMS, modelDebugConfig.getTerms());
            update.put(JobDetails.MODEL_DEBUG_CONFIG, objectMap);
        }
        else
        {
            update.put(JobDetails.MODEL_DEBUG_CONFIG, null);
        }
        m_JobProvider.updateJob(jobId, update);
    }

    public void setRenormalizationWindowDays(String jobId, Long renormalizationWindowDays) throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.RENORMALIZATION_WINDOW_DAYS, renormalizationWindowDays);
    }

    public void setModelSnapshotRetentionDays(String jobId, Long retentionDays) throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS, retentionDays);
    }

    public void setResultsRetentionDays(String jobId, Long retentionDays) throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.RESULTS_RETENTION_DAYS, retentionDays);
    }

    public void setBackgroundPersistInterval(String jobId, Long backgroundPersistInterval)
            throws UnknownJobException
    {
        updateJobTopLevelKeyValue(jobId, JobDetails.BACKGROUND_PERSIST_INTERVAL, backgroundPersistInterval);
    }

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param interimResultsParams Parameters about whether interim results calculation
     * should occur and for which period of time
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if a data upload is part way through
     */
    @Override
    public void flushJob(String jobId, InterimResultsParams interimResultsParams)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.FLUSHING))
        {
            LOGGER.debug("Flush job " + jobId);

            // First check the job is in the database.
            // this method throws if it isn't
            m_JobProvider.checkJobExists(jobId);

            m_ProcessManager.flushJob(jobId, interimResultsParams);
        }
    }

    @Override
    public void closeJob(String jobId) throws UnknownJobException, NativeProcessRunException,
            JobInUseException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.CLOSING))
        {
            LOGGER.debug("Finish job " + jobId);

            // First check the job is in the database.
            // this method throws if it isn't
            m_JobProvider.checkJobExists(jobId);

            m_JobAutoCloser.stopTimeout(jobId);
            m_ProcessManager.closeJob(jobId);
        }
    }

    public void writeUpdateConfigMessage(String jobId, String config) throws JobInUseException,
            NativeProcessRunException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
        {
            m_ProcessManager.writeUpdateConfigMessage(jobId, config);
        }
    }

    /**
     * Set time the job last received data.
     * Updates the database document with the last time data was received
     * only if the time is at least 2 seconds later than the previous time.
     * The gap is required in order to avoid too frequent updates that can
     * cause update conflicts to some storages.
     *
     * @param jobId
     * @param time
     * @return
     * @throws UnknownJobException
     */
    public void updateLastDataTime(String jobId, Date time) throws UnknownJobException
    {
        long lastDataTimeEpochMs = retrieveCachedLastDataTimeEpochMs(jobId);
        long newTimeEpochMs = time.getTime();
        if (newTimeEpochMs - lastDataTimeEpochMs >= LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS)
        {
            m_LastDataTimePerJobCache.put(jobId, newTimeEpochMs);
            updateJobTopLevelKeyValue(jobId, JobDetails.LAST_DATA_TIME, time);
        }
    }

    private long retrieveCachedLastDataTimeEpochMs(String jobId)
    {
        try
        {
            return m_LastDataTimePerJobCache.get(jobId, () -> 0L);
        }
        catch (ExecutionException e)
        {
            LOGGER.debug("Failed to load cached last data time for job: " + jobId, e);
            return 0;
        }
    }

    /**
     * Stop the associated process and remove it from the Process
     * Manager then delete the job related documents from the
     * database.
     *
     * @param jobId
     * @return
     * @throws UnknownJobException If the jobId is not recognised
     * @throws DataStoreException If there is an error deleting the job
     * @throws NativeProcessRunException
     * @throws JobInUseException If the job cannot be deleted because the
     * native process is in use.
     * @throws CannotStopSchedulerException If the job is scheduled and its scheduler fails to stop
     */
    public boolean deleteJob(String jobId) throws UnknownJobException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        LOGGER.debug("Deleting job '" + jobId + "'");

        // Try to stop scheduler before getting the Action to delete to avoid a jobInUseException
        // in case the scheduler is in the middle of submitting data
        if (isScheduledJob(jobId))
        {
            if (m_ScheduledJobs.containsKey(jobId))
            {
                JobScheduler jobScheduler = m_ScheduledJobs.get(jobId);
                if (!jobScheduler.isStopped())
                {
                    jobScheduler.stopManual();
                }
                m_ScheduledJobs.remove(jobId);
            }
            else if (m_SchedulerActionGuardian.currentAction(jobId) == ScheduledAction.START)
            {
                String msg = Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE,
                        jobId, ScheduledAction.START);
                LOGGER.info(msg);
                throw new CannotStopSchedulerException(msg);
            }
        }




        try (ActionGuardian<Action>.ActionTicket actionTicket =
                        m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.DELETING))
        {
            if (m_ProcessManager.jobIsRunning(jobId))
            {
                m_JobAutoCloser.stopTimeout(jobId);
                m_ProcessManager.closeJob(jobId);
            }

            m_ProcessManager.deletePersistedData(jobId);

            boolean success = m_JobProvider.deleteJob(jobId);
            if (success)
            {
                audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_DELETED));
            }
            return success;
        }
    }

    @Override
    public DataCounts submitDataLoadJob(String jobId, InputStream input, DataLoadParams params)
    throws UnknownJobException, NativeProcessRunException, MissingFieldException,
        JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
        OutOfOrderRecordsException, LicenseViolationException, TooManyJobsException,
        MalformedJsonException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.WRITING))
        {
            JobDetails jobDetails = getJobOrThrowIfUnknown(jobId);

            checkLicenceViolationsOnReactivate(jobDetails);
            if (jobDetails.getStatus().isAnyOf(JobStatus.PAUSING, JobStatus.PAUSED))
            {
                return new DataCounts();
            }
            DataCounts stats = tryProcessingDataLoadJob(jobDetails, input, params);
            updateLastDataTime(jobId, new Date());
            if (IgnoreDowntime.ONCE == jobDetails.getIgnoreDowntime()
                    && stats.getProcessedRecordCount() > 0)
            {
                updateIgnoreDowntime(jobId, null);
            }
            return stats;
        }
        catch (JobException e)
        {
            // Scheduled jobs perform their own auditing
            if (!isScheduledJob(jobId))
            {
                audit(jobId).error(e.getMessage());
            }
            throw e;
        }
    }

    private DataCounts tryProcessingDataLoadJob(JobDetails job, InputStream input, DataLoadParams params)
            throws UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            NativeProcessRunException, MalformedJsonException
    {
        m_JobAutoCloser.stopTimeout(job.getId());
        DataCounts stats;
        try
        {
            stats = m_ProcessManager.processDataLoadJob(job, input, params);
        }
        catch (NativeProcessRunException ne)
        {
            tryFinishingJob(job.getId());

            //rethrow
            throw ne;
        }
        finally
        {
            m_JobAutoCloser.startTimeout(job.getId(), Duration.ofSeconds(job.getTimeout()));
        }
        return stats;
    }

    public String previewTransforms(String jobId, InputStream input)
    throws JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
    OutOfOrderRecordsException, MalformedJsonException, IOException, UnknownJobException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CsvRecordWriter writer = new CsvRecordWriter(output);
        JobDetails job = getJobOrThrowIfUnknown(jobId);
        m_ProcessManager.writeToJob(false, writer, job.getDataDescription(),
                            job.getAnalysisConfig(),
                            job.getSchedulerConfig(),
                            new TransformConfigs(job.getTransforms()), input,
                            new NoneStatusReporter("preview-job"),
                            new NoneJobDataPersister(), LOGGER);
        writer.flush();
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private void checkLicenceViolationsOnReactivate(JobDetails job)
    throws LicenseViolationException, TooManyJobsException
    {
        if (getActiveJobIds().contains(job.getId()))
        {
            return;
        }

        Set<String> activeJobIds = getActiveJobIds();
        int numberOfActiveDetectors = numberOfActiveDetectors(activeJobIds);
        int numDetectorsInJob = job.getAnalysisConfig().getDetectors().size();
        m_LicenceChecker.checkLicenceViolationsOnReactivate(job.getId(),
                                                            activeJobIds.size(),
                                                            numDetectorsInJob,
                                                            numberOfActiveDetectors);
    }


    /**
     * The total number of detectors in jobs with a running process
     * and scheduled jobs in the STARTED state.
     *
     * It's possible for scheduled jobs not to have a running process.
     *
     * @param activeJobIds Set of IDs of active jobs
     * @return
     */
    private int numberOfActiveDetectors(Set<String> activeJobIds)
    {
        // the detectors of running jobs
        int numDetectors = m_ProcessManager.numberOfRunningDetectors();

        // add detectors of scheduled jobs without a running process
        Set<String> runningJobs = new HashSet<>(m_ProcessManager.runningJobs());
        numDetectors += activeJobIds.stream()
                .filter(jobId -> !runningJobs.contains(jobId))
                .map(jobId -> m_JobProvider.getJobDetails(jobId))
                .filter(optionalJob -> optionalJob.isPresent())
                .mapToInt(optionalJob -> optionalJob.get().getAnalysisConfig().getDetectors().size())
                .sum();

        return numDetectors;
    }

    /**
     * Get the job IDs of jobs that are running or have a started scheduler.
     * @return the job IDs of active jobs
     */
    public Set<String> getActiveJobIds()
    {
        Set<String> activeJobs = new HashSet<String>(m_ProcessManager.runningJobs());
        activeJobs.addAll(getStartedScheduledJobs());
        return activeJobs;
    }

    /**
     * Get the jobs that have a started scheduler
     * @return
     */
    public List<String> getStartedScheduledJobs()
    {
        List<String> startedScheduledJobs = new ArrayList<>();
        for (JobScheduler scheduler : m_ScheduledJobs.values())
        {
            if (scheduler.isStarted())
            {
                startedScheduledJobs.add(scheduler.getJobId());
            }
        }
        return startedScheduledJobs;
    }

    private void tryFinishingJob(String jobId) throws JobInUseException
    {
        try
        {
            m_ProcessManager.closeJob(jobId);
        }
        catch (NativeProcessRunException e)
        {
            LOGGER.warn("Error finishing job after submitDataLoadJob failed", e);
        }
    }

    /**
     * Get the analytics version string.
     *
     * @return
     */
    public String getAnalyticsVersion()
    {
        return  m_ProcessManager.getAnalyticsVersion();
    }

    public String apiVersion()
    {
        // Try to add extra fields (just appVer for now)
        try
        {
            // Try to get the API version as recorded by Maven at build time
            InputStream is = getClass().getResourceAsStream("/META-INF/maven/com.prelert/engine-api/pom.properties");
            if (is != null)
            {
                try
                {
                    Properties props = new Properties();
                    props.load(is);
                    return props.getProperty("version", "");
                }
                finally
                {
                    is.close();
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to load API version meta-data", e);
        }

        return "";
    }


    public void addAlertObserver(String jobId, AlertObserver ao)
    throws ClosedJobException
    {
        m_ProcessManager.addAlertObserver(jobId, ao);
    }

    public boolean removeAlertObserver(String jobId, AlertObserver ao)
    {
        return m_ProcessManager.removeAlertObserver(jobId, ao);
    }

    public void startJobScheduler(String jobId, long startMs, OptionalLong endMs)
             throws CannotStartSchedulerException, NoSuchScheduledJobException,
             UnknownJobException, TooManyJobsException, LicenseViolationException, JobInUseException
    {
        checkJobHasScheduler(jobId);

        JobDetails job = getJobOrThrowIfUnknown(jobId);
        checkLicenceViolationsOnReactivate(job);

        ActionGuardian<ScheduledAction>.ActionTicket actionTicket =
                    m_SchedulerActionGuardian.tryAcquiringAction(jobId, ScheduledAction.START);

        m_JobProvider.updateSchedulerState(jobId,
                new SchedulerState(startMs, endMs.isPresent() ? endMs.getAsLong() : null));
        LOGGER.info("Starting scheduler for job: " + jobId);

        m_ScheduledJobs.get(jobId).start(job, startMs, endMs, actionTicket);
    }

    public void stopJobScheduler(String jobId)
            throws NoSuchScheduledJobException, CannotStopSchedulerException, UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        checkJobHasScheduler(jobId);

        LOGGER.info("Stopping scheduler for job: " + jobId);

        if (m_ScheduledJobs.containsKey(jobId))
        {
            m_ScheduledJobs.get(jobId).stopManual();
            closeJob(jobId);
        }
        else if (m_SchedulerActionGuardian.currentAction(jobId) == ScheduledAction.START)
        {
            String msg = Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE,
                    jobId, ScheduledAction.START);
            LOGGER.info(msg);
            throw new CannotStopSchedulerException(msg);
        }
    }

    void checkJobHasScheduler(String jobId) throws NoSuchScheduledJobException
    {
        if (!isScheduledJob(jobId))
        {
            throw new NoSuchScheduledJobException(jobId);
        }
    }

    /**
     * True if the job has a scheduler configuration
     *
     * @param jobId
     * @return
     */
    public boolean isScheduledJob(String jobId)
    {
        Optional<JobDetails> job = m_JobProvider.getJobDetails(jobId);
        return job.isPresent() && job.get().getSchedulerConfig() != null;

        // Not necessarily correct in a distributed system
        // if a new scheduled jo b has been created on a different
        // node
//        return m_ScheduledJobs.containsKey(jobId)
//                || m_SchedulerActionGuardian.currentAction(jobId) == ScheduledAction.START;
    }

    /**
     * Creates a {@code JobScheduler} for each scheduled job and restores the state
     * of the scheduler.
     * <ul>
     * <li>If autorestart is enabled, schedulers in state STARTED will be restarted.
     * <li>If autorestart is disabled, all schedulers will be set to STOPPED.
     * <li>Schedulers in state STOPPING will be set to STOPPED.
     * </ul>
     */
    public void setupScheduledJobs()
    {
        Preconditions.checkState(m_ScheduledJobs.isEmpty());
        Boolean isAutoRestart = PrelertSettings.getSettingOrDefault(
                SCHEDULER_AUTORESTART_SETTING, DEFAULT_SCHEDULER_AUTORESTART);

        BatchedDocumentsIterator<JobDetails> jobsIterator = m_JobProvider.newBatchedJobsIterator();
        while (jobsIterator.hasNext())
        {
            for (JobDetails job : jobsIterator.next())
            {
                if (job.getSchedulerConfig() != null)
                {
                    JobScheduler jobScheduler = createJobScheduler(job);
                    if (isAutoRestart && job.getSchedulerStatus() == JobSchedulerStatus.STARTED)
                    {
                        restartScheduledJob(job, jobScheduler);
                    }
                    else if (!isAutoRestart || job.getSchedulerStatus() == JobSchedulerStatus.STOPPING)
                    {
                        restoreSchedulerStatusToStopped(job);
                    }
                }
            }
        }
    }

    /**
     * Restarts the scheduler and suppresses any errors
     * e.g. if the scheduler can't start because it's running elsewhere
     *
     * @param job
     * @param scheduler
     */
    private void restartScheduledJob(JobDetails job, JobScheduler scheduler)
    {
        Optional<SchedulerState> optionalSchedulerState = m_JobProvider.getSchedulerState(job.getId());
        if (!optionalSchedulerState.isPresent())
        {
            LOGGER.error("Failed to restart scheduler for job: " + job.getId()
                    + ". No schedulerState could be found.");
            return;
        }
        SchedulerState schedulerState = optionalSchedulerState.get();
        long startTimeMs = schedulerState.getStartTimeMillis() == null ? 0
                : schedulerState.getStartTimeMillis();
        OptionalLong endTimeMs = schedulerState.getEndTimeMillis() == null ? OptionalLong.empty()
                : OptionalLong.of(schedulerState.getEndTimeMillis());

        try
        {
            ActionGuardian<ScheduledAction>.ActionTicket actionTicket =
                    m_SchedulerActionGuardian.tryAcquiringAction(job.getId(),
                                                                ScheduledAction.START);

            LOGGER.info("Starting scheduler for job: " + job.getId());
            scheduler.start(job, startTimeMs, endTimeMs, actionTicket);
        }
        catch (CannotStartSchedulerException | JobInUseException e)
        {
            LOGGER.error("Failed to restart scheduler for job: " + job.getId(), e);
        }
    }

    private void restoreSchedulerStatusToStopped(JobDetails job)
    {
        try
        {
            updateJobTopLevelKeyValue(job.getId(), JobDetails.SCHEDULER_STATUS,
                    JobSchedulerStatus.STOPPED);
        }
        catch (UnknownJobException e)
        {
            LOGGER.error("An error occurred while restoring scheduler status for job "
                    + e.getJobId(), e);
        }
    }

    public boolean updateDetectorDescription(String jobId, int detectorIndex, String newDescription)
            throws UnknownJobException
    {
        return m_JobProvider.updateDetectorDescription(jobId, detectorIndex, newDescription);
    }

    public boolean updateSchedulerConfig(String jobId, SchedulerConfig newSchedulerConfig)
            throws JobException
    {
        try (ActionGuardian<Action>.ActionTicket actionTicket =
                                m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
        {
            checkJobHasScheduler(jobId);
            JobScheduler scheduler = m_ScheduledJobs.get(jobId);
            if (scheduler != null && !scheduler.isStopped())
            {
                throw new CannotUpdateSchedulerException(jobId, scheduler.getStatus());
            }

            if (m_SchedulerActionGuardian.currentAction(jobId) == ScheduledAction.START)
            {
                throw new CannotUpdateSchedulerException(jobId, scheduler.getStatus());
            }

            securePassword(newSchedulerConfig);
            if (m_JobProvider.updateSchedulerConfig(jobId, newSchedulerConfig))
            {
                JobDetails job = getJobOrThrowIfUnknown(jobId);
                // We cannot be sure the update is available at the storage
                // so we set the new schedulerConfig explicitly.
                job.setSchedulerConfig(newSchedulerConfig);
                createJobScheduler(job);
                return true;
            }
            return false;
        }
    }

    @Override
    public void shutdown()
    {
        for (String jobId : m_ScheduledJobs.keySet())
        {
            m_ScheduledJobs.get(jobId).stopAuto();
        }
        m_ScheduledJobs.clear();

        m_JobAutoCloser.shutdown();

        systemAudit().info(Messages.getMessage(Messages.SYSTEM_AUDIT_SHUTDOWN));
    }

    public Auditor audit(String jobId)
    {
        return m_JobProvider.audit(jobId);
    }

    public Auditor systemAudit()
    {
        return m_JobProvider.audit("");
    }

    /**
     * Sets {@link IgnoreDowntime#ONCE} to all jobs that satisfy the following criteria:
     * <ul>
     * <li>have no {@code ignoreDowntime} set
     * <li>have at least one processed record
     * </ul>
     */
    public void setIgnoreDowntimeToAllJobs()
    {
        LOGGER.info("Setting ignoreDowntime to all jobs");
        BatchedDocumentsIterator<JobDetails> jobsIterator = m_JobProvider.newBatchedJobsIterator();
        while (jobsIterator.hasNext())
        {
            for (JobDetails job : jobsIterator.next())
            {
                // Only set if job has seen data
                DataCounts counts = job.getCounts();
                boolean hasSeenData = counts != null && counts.getProcessedRecordCount() > 0;
                if (job.getIgnoreDowntime() == null && hasSeenData)
                {
                    try
                    {
                        updateIgnoreDowntime(job.getId(), IgnoreDowntime.ONCE);
                    }
                    catch (UnknownJobException e)
                    {
                        LOGGER.error("Could not set ignoreDowntime on job " + e.getJobId(), e);
                    }
                }
            }
        }
    }

    /**
     * Pauses a job. While a job is paused, any data submitted to it will be
     * accepted but not processed. After resuming a job, the gap between the latest
     * processed data and the first new data will be ignored, i.e. no results will
     * be created for the gap and the models will be unaffected.
     *
     * @param jobId the job id
     * @throws JobInUseException if the job is used by another action
     * @throws NativeProcessRunException if the native process fails while closing the job
     * @throws UnknownJobException if there is no job for the given {@code jobId}
     * @throws CannotPauseJobException if the job cannot be paused in its current state
     */
    public void pauseJob(String jobId) throws JobInUseException, NativeProcessRunException,
            UnknownJobException, CannotPauseJobException
    {
        Preconditions.checkState(!isScheduledJob(jobId));
        LOGGER.info("Pausing job " + jobId);

        try (ActionGuardian<Action>.ActionTicket actionTicket =
                            m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.PAUSING))
        {
            JobDetails job = getJobOrThrowIfUnknown(jobId);
            if (!job.getStatus().isAnyOf(JobStatus.RUNNING, JobStatus.CLOSED))
            {
                throw new CannotPauseJobException(jobId, job.getStatus());
            }

            if (m_ProcessManager.jobIsRunning(jobId))
            {
                m_JobAutoCloser.stopTimeout(jobId);
                m_ProcessManager.closeJob(jobId);
            }

            m_JobProvider.setJobStatus(jobId, JobStatus.PAUSING);
            updateIgnoreDowntime(jobId, IgnoreDowntime.ONCE);
            m_JobProvider.setJobStatus(jobId, JobStatus.PAUSED);

            audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_PAUSED));
        }
    }

    /**
     * Resumes a job.
     *
     * @param jobId the job id
     * @throws JobInUseException if the job is used by another action
     * @throws UnknownJobException if there is no job for the given {@code jobId}
     * @throws CannotResumeJobException if the job cannot be resumed in its current state
     */
    public void resumeJob(String jobId)
            throws JobInUseException, UnknownJobException, CannotResumeJobException
    {
        Preconditions.checkState(!isScheduledJob(jobId));
        LOGGER.info("Resuming job " + jobId);

        try (ActionGuardian<Action>.ActionTicket actionTicket =
                                   m_ProcessActionGuardian.tryAcquiringAction(jobId, Action.RESUMING))
        {
            JobDetails job = getJobOrThrowIfUnknown(jobId);
            if (job.getStatus() != JobStatus.PAUSED)
            {
                throw new CannotResumeJobException(jobId, job.getStatus());
            }

            m_JobProvider.setJobStatus(jobId, JobStatus.CLOSED);
            audit(jobId).info(Messages.getMessage(Messages.JOB_AUDIT_RESUMED));
        }
    }

    public List<JobDetails> getAllJobs()
    {
        BatchedDocumentsIterator<JobDetails> jobsIterator = m_JobProvider.newBatchedJobsIterator();
        List<JobDetails> allJobs = new ArrayList<>();
        while (jobsIterator.hasNext())
        {
            allJobs.addAll(jobsIterator.next());
        }
        return allJobs;
    }

    public List<JobDetails> getActiveJobs()
    {
        Set<String> jobIds = getActiveJobIds();
        List<JobDetails> activeJobs = new ArrayList<>();
        for (String id : jobIds)
        {
            Optional<JobDetails> jd = m_JobProvider.getJobDetails(id);
            if (jd.isPresent())
            {
                activeJobs.add(jd.get());
            }
        }

        return activeJobs;
    }
}
