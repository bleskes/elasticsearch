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
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
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
import com.prelert.job.DataCounts;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.audit.Auditor;
import com.prelert.job.config.DefaultFrequency;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.data.extraction.DataExtractorFactory;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;
import com.prelert.job.manager.actions.ActionGuardian.ActionTicket;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
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
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
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

    /**
     * The default number of documents returned in queries as a string.
     */
    public static final String DEFAULT_PAGE_SIZE_STR = "100";

    /**
     * The default number of documents returned in queries.
     */
    public static final int DEFAULT_PAGE_SIZE;
    static
    {
        DEFAULT_PAGE_SIZE = Integer.parseInt(DEFAULT_PAGE_SIZE_STR);
    }

    public static final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY;

    private static final String MAX_JOBS_FACTOR_NAME = "max.jobs.factor";
    private static final double DEFAULT_MAX_JOBS_FACTOR = 6.0;

    private static final int LAST_DATA_TIME_CACHE_SIZE = 1000;
    private static final int LAST_DATA_TIME_MIN_UPDATE_INTERVAL_MS = 1000;

    private static final int MAX_JOBS_TO_RESTART = 10000;

    /**
     * constraints in the license key.
     */
    public static final String JOBS_LICENSE_CONSTRAINT = "jobs";
    public static final String DETECTORS_LICENSE_CONSTRAINT = "detectors";
    public static final String PARTITIONS_LICENSE_CONSTRAINT = "partitions";

    private final ActionGuardian m_ActionGuardian;
    private final JobProvider m_JobProvider;
    private final ProcessManager m_ProcessManager;
    private final DataExtractorFactory m_DataExtractorFactory;
    private final JobLoggerFactory m_JobLoggerFactory;
    private final JobTimeouts m_JobTimeouts;

    private final JobFactory m_JobFactory;
    private final int m_MaxAllowedJobs;
    private final BackendInfo m_BackendInfo;

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
            DataExtractorFactory dataExtractorFactory, JobLoggerFactory jobLoggerFactory)
    {
        m_ActionGuardian = new ActionGuardian();
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_ProcessManager = Objects.requireNonNull(processManager);
        m_DataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_JobTimeouts = new JobTimeouts(jobId -> closeJob(jobId));

        m_MaxAllowedJobs = calculateMaxJobsAllowed();

        m_ScheduledJobs = new ConcurrentHashMap<>();
        m_LastDataTimePerJobCache = CacheBuilder.newBuilder()
                .maximumSize(LAST_DATA_TIME_CACHE_SIZE)
                .build();

        // This requires the process manager and data storage connection in
        // order to work, but failure is considered non-fatal
        m_BackendInfo = BackendInfo.fromJson(m_ProcessManager.getInfo(), m_JobProvider, apiVersion());
        m_JobFactory = new JobFactory(m_BackendInfo);
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
     * Get the details of the specific job wrapped in a <code>Optional</code>
     *
     * @param jobId
     * @return An {@code Optional} containing the {@code JobDetails} if a job with the given
     * {@code jobId} exists, or an empty {@code Optional} otherwise
     */
    public Optional<JobDetails> getJob(String jobId)
    {
        return m_JobProvider.getJobDetails(jobId);
    }

    /**
     * Get details of all Jobs.
     *
     * @param skip Skip the first N Jobs. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of Jobs
     * @return A pagination object with hitCount set to the total number
     * of jobs not the only the number returned here as determined by the
     * <code>take</code>
     * parameter.
     */
    public QueryPage<JobDetails> getJobs(int skip, int take)
    {
        return m_JobProvider.getJobs(skip, take);
    }

    /**
     * Create a new job from the configuration object and insert into the
     * document store. The details of the newly created job are returned.
     *
     * @param jobConfig
     * @return The new job or <code>null</code> if an exception occurs.
     * @throws TooManyJobsException If the license is violated
     * @throws JobConfigurationException If the license is violated
     * @throws JobIdAlreadyExistsException If the alias is already taken
     */
    public JobDetails createJob(JobConfiguration jobConfig)
            throws TooManyJobsException, JobConfigurationException, JobIdAlreadyExistsException
    {
        JobDetails jobDetails = m_JobFactory.create(jobConfig, m_ProcessManager.numberOfRunningJobs());

        if (!m_JobProvider.jobIdIsUnique(jobDetails.getId()))
        {
            throw new JobIdAlreadyExistsException(jobDetails.getId());
        }

        m_JobProvider.createJob(jobDetails);
        audit(jobDetails.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_CREATED));

        if (jobDetails.getSchedulerConfig() != null)
        {
            createJobScheduler(jobDetails);
        }

        return jobDetails;
    }

    private JobScheduler createJobScheduler(JobDetails job)
    {
        Duration bucketSpan = Duration.ofSeconds(job.getAnalysisConfig().getBucketSpan());
        Duration frequency = getFrequencyOrDefault(job);
        Duration queryDelay = Duration.ofSeconds(job.getSchedulerConfig().getQueryDelay());
        JobScheduler jobScheduler = new JobScheduler(job.getId(), bucketSpan, frequency, queryDelay,
                m_DataExtractorFactory.newExtractor(job), this, m_JobProvider, m_JobLoggerFactory);
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

    /**
     * Get a single result bucket
     *
     * @param jobId
     * @param bucketId
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public Optional<Bucket> bucket(String jobId,
            String bucketId, boolean expand, boolean includeInterim)
    throws NativeProcessRunException, UnknownJobException
    {
         Optional<Bucket> result = m_JobProvider.bucket(jobId, bucketId, expand, includeInterim);

        if (result.isPresent() && !expand)
        {
            result.get().setRecords(null);
        }

        return result;
    }


    /**
     * Get result buckets
     *
     * @param jobId
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @param skip
     * @param take
     * @param anomalyScoreThreshold
     * @param normalizedProbabilityThreshold
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryPage<Bucket> buckets = m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.queryResults())
            {
                bucket.setRecords(null);
            }
        }

        return buckets;
    }


    /**
     * Get result buckets between 2 dates
     * @param jobId
     * @param expand Include anomaly records. If false the bucket's records
     *  are set to <code>null</code> so they aren't serialised
     * @param includeInterim Include interim results
     * @param skip
     * @param take
     * @param startEpochMs Return buckets starting at this time
     * @param endBucketMs Include buckets up to this time
     * @param anomalyScoreThreshold
     * @param normalizedProbabilityThreshold
     * @return
     * @throws UnknownJobException
     */
    public QueryPage<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take, long startEpochMs, long endBucketMs,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryPage<Bucket> buckets =  m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, startEpochMs, endBucketMs,
                anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.queryResults())
            {
                bucket.setRecords(null);
            }
        }

        return buckets;
    }

    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take,
            long epochStartMs, long epochEndMs, String sortField, String description)
            throws UnknownJobException
    {
        return m_JobProvider.modelSnapshots(jobId, skip, take, epochStartMs, epochEndMs,
                sortField, null, description);
    }

    public ModelSnapshot revertToSnapshot(String jobId, long epochEndMs, String snapshotId,
            String description)
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.REVERTING))
        {
            LOGGER.debug("Reverting model snapshot for job '" + jobId + "'");

            if (m_ProcessManager.jobIsRunning(jobId))
            {
                throw new JobInUseException(Messages.getMessage(Messages.REST_JOB_NOT_CLOSED_REVERT),
                        ErrorCodes.JOB_NOT_CLOSED);
            }

            List<ModelSnapshot> revertCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, epochEndMs, ModelSnapshot.TIMESTAMP, snapshotId, description).queryResults();

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

            updateIgnoreDowntime(jobId, IgnoreDowntime.ONCE);
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
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
        {
            List<ModelSnapshot> changeCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, 0, null, snapshotId, null).queryResults();

            if (changeCandidates == null || changeCandidates.isEmpty())
            {
                throw new NoSuchModelSnapshotException(jobId);
            }

            List<ModelSnapshot> clashCandidates = m_JobProvider.modelSnapshots(jobId, 0, 1,
                    0, 0, null, null, newDescription).queryResults();

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

    public QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
            throws UnknownJobException
    {
        return m_JobProvider.categoryDefinitions(jobId, skip, take);
    }

    public Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
            throws UnknownJobException
    {
        return m_JobProvider.categoryDefinition(jobId, categoryId);
    }

    public QueryPage<Influencer> influencers(String jobId, int skip, int take, long epochStartMs,
            long epochEndMs, String sortField, boolean sortDescending, double anomalyScoreFilter,
            boolean includeInterim)
            throws UnknownJobException
    {
        return m_JobProvider.influencers(jobId, skip, take, epochStartMs, epochEndMs, sortField,
                sortDescending, anomalyScoreFilter, includeInterim);
    }

    public Optional<Influencer> influencer(String jobId, String influencerId)
            throws UnknownJobException
    {
        return m_JobProvider.influencer(jobId, influencerId);
    }

    /**
     * Get a page of anomaly records from all buckets.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * results if not required set to 0.
     * @param take Take only this number of records
     * @param includeInterim Include interim results
     * @param sortField The field to sort by
     * @param sortDescending
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws NativeProcessRunException, UnknownJobException
    {
        return m_JobProvider.records(jobId, skip, take, includeInterim, sortField, sortDescending,
                                            anomalyScoreThreshold, normalizedProbabilityThreshold);
    }


    /**
     * Get a page of anomaly records from the buckets between
     * epochStart and epochEnd.
     *
     * @param jobId
     * @param skip
     * @param take
     * @param epochStartMs
     * @param epochEndMs
     * @param includeInterim Include interim results
     * @param sortField
     * @param sortDescending
     * @param anomalyScoreThreshold Return only buckets with an anomalyScore >=
     * this value
     * @param normalizedProbabilityThreshold Return only buckets with a maxNormalizedProbability >=
     * this value
     *
     * @return
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, long epochStartMs, long epochEndMs,
            boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws NativeProcessRunException, UnknownJobException
    {
        return m_JobProvider.records(jobId, skip, take,
                            epochStartMs, epochEndMs, includeInterim, sortField, sortDescending,
                            anomalyScoreThreshold, normalizedProbabilityThreshold);
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
    public void flushJob(String jobId, InterimResultsParams interimResultsParams)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.FLUSHING))
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
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.CLOSING))
        {
            LOGGER.debug("Finish job " + jobId);

            // First check the job is in the database.
            // this method throws if it isn't
            m_JobProvider.checkJobExists(jobId);

            m_JobTimeouts.stopTimeout(jobId);
            m_ProcessManager.closeJob(jobId);
        }
    }

    public void writeUpdateConfigMessage(String jobId, String config) throws JobInUseException,
            NativeProcessRunException
    {
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.UPDATING))
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
        if (m_ScheduledJobs.containsKey(jobId))
        {
            JobScheduler jobScheduler = m_ScheduledJobs.get(jobId);
            if (jobScheduler.isStarted())
            {
                jobScheduler.stopManual();
            }
            m_ScheduledJobs.remove(jobId);
        }

        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.DELETING))
        {
            if (m_ProcessManager.jobIsRunning(jobId))
            {
                m_JobTimeouts.stopTimeout(jobId);
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
        OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        try (ActionTicket actionTicket = m_ActionGuardian.tryAcquiringAction(jobId, Action.WRITING))
        {
            checkTooManyJobs(jobId);
            Optional<JobDetails> jobDetails = m_JobProvider.getJobDetails(jobId);
            if (!jobDetails.isPresent())
            {
                throw new UnknownJobException(jobId);
            }
            DataCounts stats = tryProcessingDataLoadJob(jobDetails.get(), input, params);
            updateLastDataTime(jobId, new Date());
            if (IgnoreDowntime.ONCE == jobDetails.get().getIgnoreDowntime()
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
        m_JobTimeouts.stopTimeout(job.getId());
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
            m_JobTimeouts.startTimeout(job.getId(), Duration.ofSeconds(job.getTimeout()));
        }
        return stats;
    }

    public String previewTransforms(String jobId, InputStream input)
    throws JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
    OutOfOrderRecordsException, MalformedJsonException, IOException, UnknownJobException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CsvRecordWriter writer = new CsvRecordWriter(output);
        Optional<JobDetails> result = m_JobProvider.getJobDetails(jobId);
        if (result.isPresent() == false)
        {
            throw new UnknownJobException(jobId);
        }

        JobDetails job = result.get();
        m_ProcessManager.writeToJob(writer, job.getDataDescription(),
                            job.getAnalysisConfig(),
                            job.getSchedulerConfig(),
                            new TransformConfigs(job.getTransforms()), input,
                            new NoneStatusReporter("preview-job"),
                            new NoneJobDataPersister(), LOGGER);

        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private void checkTooManyJobs(String jobId) throws TooManyJobsException
    {
        if (m_ProcessManager.jobIsRunning(jobId))
        {
            return;
        }
        checkTooManyJobsAgainstHardLimit(jobId);
        checkDataLoadForTooManyJobsAgainstLicenseLimit(jobId);
    }

    private void checkTooManyJobsAgainstHardLimit(String jobId) throws TooManyJobsException
    {
        if (m_ProcessManager.numberOfRunningJobs() >= m_MaxAllowedJobs)
        {
            String message = Messages.getMessage(Messages.CPU_LIMIT_JOB, jobId);

            LOGGER.info(message);
            throw new TooManyJobsException(m_MaxAllowedJobs, message,
                    ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY);
        }
    }

    private void checkDataLoadForTooManyJobsAgainstLicenseLimit(String jobId)
            throws TooManyJobsException
    {
        if (m_BackendInfo.isLicenseJobLimitViolated(m_ProcessManager.numberOfRunningJobs()))
        {
            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS_REACTIVATE,
                                        jobId, m_BackendInfo.getLicenseJobLimit());

            LOGGER.info(message);
            throw new TooManyJobsException(m_BackendInfo.getLicenseJobLimit(), message,
                    ErrorCodes.LICENSE_VIOLATION);
        }
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
            throws CannotStartSchedulerException, NoSuchScheduledJobException, UnknownJobException
    {
        checkJobHasScheduler(jobId);

        m_JobProvider.updateSchedulerState(jobId,
                new SchedulerState(startMs, endMs.isPresent() ? endMs.getAsLong() : null));

        JobDetails job = getJob(jobId).get();
        LOGGER.info("Starting scheduler for job: " + jobId);
        m_ScheduledJobs.get(jobId).start(job, startMs, endMs);
    }

    public void stopJobScheduler(String jobId)
            throws NoSuchScheduledJobException, CannotStopSchedulerException, UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        checkJobHasScheduler(jobId);
        LOGGER.info("Stopping scheduler for job: " + jobId);
        m_ScheduledJobs.get(jobId).stopManual();
        closeJob(jobId);
    }

    void checkJobHasScheduler(String jobId) throws NoSuchScheduledJobException
    {
        if (!isScheduledJob(jobId))
        {
            throw new NoSuchScheduledJobException(jobId);
        }
    }

    public boolean isScheduledJob(String jobId)
    {
        return m_ScheduledJobs.containsKey(jobId);
    }

    public void restartScheduledJobs()
    {
        Preconditions.checkState(m_ScheduledJobs.isEmpty());

        for (JobDetails job : getJobs(0, MAX_JOBS_TO_RESTART).queryResults())
        {
            if (job.getSchedulerConfig() != null)
            {
                JobScheduler jobScheduler = createJobScheduler(job);
                if (job.getSchedulerStatus() == JobSchedulerStatus.STARTED)
                {
                    restartScheduledJob(job, jobScheduler);
                }
            }
        }
    }

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
            LOGGER.info("Starting scheduler for job: " + job.getId());
            scheduler.start(job, startTimeMs, endTimeMs);
        }
        catch (CannotStartSchedulerException e)
        {
            LOGGER.error("Failed to restart scheduler for job: " + job.getId(), e);
        }
    }

    private static int calculateMaxJobsAllowed()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        double factor = readMaxJobsFactorOrDefault();
        return (int) Math.ceil(cores * factor);
    }

    private static double readMaxJobsFactorOrDefault()
    {
        String readMaxJobsFactor = PrelertSettings.getSettingText(MAX_JOBS_FACTOR_NAME, Double.toString(DEFAULT_MAX_JOBS_FACTOR));
        try
        {
            return Double.parseDouble(readMaxJobsFactor);
        }
        catch (NumberFormatException e)
        {
            LOGGER.warn(String.format(
                    "Max jobs factor is invalid: %s. Default value of %f is used.",
                    readMaxJobsFactor, DEFAULT_MAX_JOBS_FACTOR));
            return DEFAULT_MAX_JOBS_FACTOR;
        }
    }

    public boolean updateDetectorDescription(String jobId, int detectorIndex, String newDescription)
            throws UnknownJobException
    {
        return m_JobProvider.updateDetectorDescription(jobId, detectorIndex, newDescription);
    }

    @Override
    public void shutdown()
    {
        for (String jobId : m_ScheduledJobs.keySet())
        {
            m_ScheduledJobs.get(jobId).stopAuto();
        }
        m_ScheduledJobs.clear();

        m_JobTimeouts.shutdown();

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

    public void setIgnoreDowntimeToAllJobs()
    {
        LOGGER.info("Setting ignoreDowntime to all jobs");
        for (JobDetails job : getJobs(0, MAX_JOBS_TO_RESTART).queryResults())
        {
            // Only set if job has seen data
            DataCounts counts = job.getCounts();
            if (counts != null && counts.getProcessedRecordCount() > 0)
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
