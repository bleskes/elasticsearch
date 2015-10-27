/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prelert.job.DataCounts;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
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
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.none.NoneStatusReporter;
import com.prelert.job.transform.TransformConfigs;


/**
 * Creates jobs and handles retrieving job configuration details from
 * the data store. New jobs have a unique job id see {@linkplain #generateJobId()}
 */
public class JobManager
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

    private static final String MAX_JOBS_FACTOR_NAME = "prelert.max.jobs.factor";
    private static final double DEFAULT_MAX_JOBS_FACTOR = 3.0;

    private final ProcessManager m_ProcessManager;


    private AtomicLong m_IdSequence;
    private DateFormat m_JobIdDateFormat;

    private ObjectMapper m_ObjectMapper;

    private JobProvider m_JobProvider;

    private final int m_MaxAllowedJobs;

    /**
     * These default to unlimited (indicated by negative limits), but may be
     * overridden by constraints in the license key.
     */
    private int m_LicenseJobLimit = -1;
    private int m_MaxDetectorsPerJob = -1;

    /**
     * The constraint on whether partition fields are allowed.
     * See https://anomaly.atlassian.net/wiki/display/EN/Electronic+license+keys
     * and bug 1034 in Bugzilla for background.
     */
    private boolean m_ArePartitionsAllowed = true;

    /**
     * constraints in the license key.
     */
    public static final String JOBS_LICENSE_CONSTRAINT = "jobs";
    public static final String DETECTORS_LICENSE_CONSTRAINT = "detectors";
    public static final String PARTITIONS_LICENSE_CONSTRAINT = "partitions";

    /**
     * Create a JobManager
     *
     * @param jobDetailsProvider
     */
    public JobManager(JobProvider jobProvider, ProcessManager processManager)
    {
        m_JobProvider = jobProvider;

        m_ProcessManager = processManager;

        m_MaxAllowedJobs = calculateMaxJobsAllowed();

        m_IdSequence = new AtomicLong();
        m_JobIdDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        m_ObjectMapper = new ObjectMapper();
        m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // This requires the process manager and Elasticsearch connection in
        // order to work, but failure is considered non-fatal
        saveInfo();
    }


    /**
     * Get the details of the specific job wrapped in a <code>Optional</code>
     *
     * @param jobId
     * @return The JobDetails or throws UnknownJobException
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
     * @throws UnknownJobException
     * @throws IOException
     * @throws TooManyJobsException If the license is violated
     * @throws JobConfigurationException If the license is violated
     * @throws JobIdAlreadyExistsException If the alias is already taken
     */
    public JobDetails createJob(JobConfiguration jobConfig) throws UnknownJobException,
            IOException, TooManyJobsException, JobConfigurationException,
            JobIdAlreadyExistsException
    {
        checkCreateJobForTooManyJobsAgainstLicenseLimit();

        // Negative m_MaxDetectorsPerJob means unlimited
        if (m_MaxDetectorsPerJob >= 0 &&
            jobConfig.getAnalysisConfig() != null &&
            jobConfig.getAnalysisConfig().getDetectors().size() > m_MaxDetectorsPerJob)
        {

            String message = Messages.getMessage(
                                Messages.LICENSE_LIMIT_DETECTORS,
                                m_MaxDetectorsPerJob,
                                jobConfig.getAnalysisConfig().getDetectors().size());

            LOGGER.info(message);
            throw new JobConfigurationException(message, ErrorCodes.LICENSE_VIOLATION);
        }

        if (!m_ArePartitionsAllowed && jobConfig.getAnalysisConfig() != null)
        {
            for (com.prelert.job.Detector detector :
                        jobConfig.getAnalysisConfig().getDetectors())
            {
                String partitionFieldName = detector.getPartitionFieldName();
                if (partitionFieldName != null &&
                    partitionFieldName.length() > 0)
                {
                    String message = Messages.getMessage(Messages.LICENSE_LIMIT_PARTITIONS);
                    LOGGER.info(message);
                    throw new JobConfigurationException(message, ErrorCodes.LICENSE_VIOLATION);
                }
            }
        }

        String jobId = jobConfig.getId();
        if (jobId == null || jobId.isEmpty())
        {
            jobId = generateJobId();
        }
        else
        {
            if (!m_JobProvider.jobIdIsUnique(jobId))
            {
                throw new JobIdAlreadyExistsException(jobId);
            }
        }

        JobDetails jobDetails;

        if (jobConfig.getReferenceJobId() != null &&
                jobConfig.getReferenceJobId().isEmpty() == false)
        {
            JobDetails referenced = getReferencedJob(jobConfig.getReferenceJobId());
            jobDetails = new JobDetails(jobId, referenced, jobConfig);
        }
        else
        {
            jobDetails = new JobDetails(jobId, jobConfig);
        }

        m_JobProvider.createJob(jobDetails);

        return jobDetails;
    }

    private void checkCreateJobForTooManyJobsAgainstLicenseLimit() throws TooManyJobsException
    {
        if (areMoreJobsRunningThanLicenseLimit())
        {
            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS, m_LicenseJobLimit);

            LOGGER.info(message);
            throw new TooManyJobsException(m_LicenseJobLimit, message, ErrorCodes.LICENSE_VIOLATION);
        }
    }

    private boolean areMoreJobsRunningThanLicenseLimit()
    {
        // Negative m_LicenseJobLimit means unlimited
        return m_LicenseJobLimit >= 0 &&
                m_ProcessManager.numberOfRunningJobs() >= m_LicenseJobLimit;
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
            long epochEndMs, String sortField, boolean sortDescending, double anomalyScoreFilter)
            throws UnknownJobException
    {
        return m_JobProvider.influencers(jobId, skip, take, epochStartMs, epochEndMs, sortField,
                sortDescending, anomalyScoreFilter);
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
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.DESCRIPTION, description);
        m_JobProvider.updateJob(jobId, update);
    }

    public void setModelDebugConfig(String jobId, ModelDebugConfig modelDebugConfig)
            throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        if (modelDebugConfig != null)
        {
            Map<String, Object> objectMap = new HashMap<>();
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
        LOGGER.debug("Flush job " + jobId);

        // First check the job is in the database.
        // this method throws if it isn't
        m_JobProvider.checkJobExists(jobId);

        m_ProcessManager.flushJob(jobId, interimResultsParams);
    }

    /**
     * Stop the running job and mark it as finished.<br>
     *
     * @param jobId The job to stop
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if the job cannot be closed because data is
     * being streamed to it
     */
    public void closeJob(String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Finish job " + jobId);

        // First check the job is in the database.
        // this method throws if it isn't
        m_JobProvider.checkJobExists(jobId);

        m_ProcessManager.closeJob(jobId);
    }

    /**
     * Set time the job last received data.
     * Updates the database document
     *
     * @param jobId
     * @param time
     * @return
     * @throws UnknownJobException
     */
    private boolean updateLastDataTime(String jobId, Date time)
    throws UnknownJobException
    {

        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.LAST_DATA_TIME, time);
        update.put(JobDetails.STATUS, JobStatus.RUNNING);
        return m_JobProvider.updateJob(jobId, update);
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
     */
    public boolean deleteJob(String jobId)
    throws UnknownJobException, DataStoreException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Deleting job '" + jobId + "'");

        if (m_ProcessManager.jobIsRunning(jobId))
        {
            m_ProcessManager.closeJob(jobId);
        }

        m_ProcessManager.deletePersistedData(jobId);

        return m_JobProvider.deleteJob(jobId);
    }

    /**
     * Passes data to the native process. If the process is not running a new
     * one is started.
     * This is a blocking call that won't return until all the data has been
     * written to the process. A new thread is launched to parse the process's
     * output
     *
     * @param jobId
     * @param input
     * @param params
     * @return
     * @throws NativeProcessRunException If there is an error starting the native
     * process
     * @throws UnknownJobException If the jobId is not recognised
     * @throws MissingFieldException If a configured field is missing from
     * the CSV header
     * @throws JsonParseException
     * @throws JobInUseException if the job cannot be written to because
     * it is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws TooManyJobsException If the license is violated
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     * @return Count of records, fields, bytes, etc written
     */
    public DataCounts submitDataLoadJob(String jobId, InputStream input, DataLoadParams params)
    throws UnknownJobException, NativeProcessRunException, MissingFieldException,
        JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
        OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        checkTooManyJobs(jobId);
        DataCounts stats = tryProcessingDataLoadJob(jobId, input, params);
        updateLastDataTime(jobId, new Date());
        return stats;
    }

    private DataCounts tryProcessingDataLoadJob(String jobId, InputStream input, DataLoadParams params)
            throws UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            NativeProcessRunException, MalformedJsonException
    {
        DataCounts stats;
        try
        {
            stats = m_ProcessManager.processDataLoadJob(jobId, input, params);
        }
        catch (NativeProcessRunException ne)
        {
            tryFinishingJob(jobId);

            //rethrow
            throw ne;
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
                            new TransformConfigs(job.getTransforms()), input,
                            new NoneStatusReporter("preview-job"),
                            new NoneJobDataPersister(), LOGGER);

        String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
        return text;
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
        if (areMoreJobsRunningThanLicenseLimit())
        {
            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS_REACTIVATE,
                                        jobId, m_LicenseJobLimit);

            LOGGER.info(message);
            throw new TooManyJobsException(m_LicenseJobLimit, message, ErrorCodes.LICENSE_VIOLATION);
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
     * The job id is a concatenation of the date in 'yyyyMMddHHmmss' format
     * and a sequence number that is a minimum of 5 digits wide left padded
     * with zeros.<br>
     * e.g. the first Id created 23rd November 2013 at 11am
     *     '20131125110000-00001'
     *
     * @return The new unique job Id
     */
    private String generateJobId()
    {
        return String.format("%s-%05d", m_JobIdDateFormat.format(new Date()),
                        m_IdSequence.incrementAndGet());
    }

    /**
     * Stops the Elasticsearch client and the Process Manager
     */
    public void stop()
    {
        m_ProcessManager.stop();
        try
        {
            m_JobProvider.close();
        }
        catch (IOException e)
        {
            LOGGER.error("Exception closing job details provider", e);
        }
    }

    /**
     * Get the Job details for the job Id. If the job cannot be found
     * <code>null</code> is returned.
     *
     * @param refId
     * @return <code>null</code> or the job details
     * @throws UnknownJobException If there is no previously created
     * job with the id <code>refId</code>
     */
    private JobDetails getReferencedJob(String refId)
    throws UnknownJobException
    {
        Optional<JobDetails> job = m_JobProvider.getJobDetails(refId);

        if (job.isPresent() == false)
        {
            String message = Messages.getMessage(Messages.JOB_UNKNOWN_REFERENCE, refId);

            LOGGER.info(message);
            throw new UnknownJobException(refId, message, ErrorCodes.UNKNOWN_JOB_REFERENCE);
        }

        return job.get();
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

    /**
     * Attempt to get usage and license info from the C++ process, add extra
     * fields and persist to Elasticsearch.  Any failures are logged but do not
     * otherwise impact operation of this process.  Additionally, any license
     * constraints are extracted from the same info document.
     */
    private void saveInfo()
    {
        // This will be a JSON document in string form
        String backendInfo = m_ProcessManager.getInfo();

        // Try to parse the string returned from the C++ process and extract
        // any license constraints
        ObjectNode doc;
        try
        {
            doc = (ObjectNode)m_ObjectMapper.readTree(backendInfo);

            // Negative numbers indicate no constraint, i.e. unlimited maximums
            JsonNode constraint = doc.get(JOBS_LICENSE_CONSTRAINT);
            if (constraint != null)
            {
                m_LicenseJobLimit = constraint.asInt(-1);
            }
            else
            {
                m_LicenseJobLimit = -1;
            }
            LOGGER.info("License job limit = " + m_LicenseJobLimit);
            constraint = doc.get(DETECTORS_LICENSE_CONSTRAINT);
            if (constraint != null)
            {
                m_MaxDetectorsPerJob = constraint.asInt(-1);
            }
            else
            {
                m_MaxDetectorsPerJob = -1;
            }
            LOGGER.info("Max detectors per job = " + m_MaxDetectorsPerJob);
            constraint = doc.get(PARTITIONS_LICENSE_CONSTRAINT);
            if (constraint != null)
            {
                int val = constraint.asInt(-1);
                // See https://anomaly.atlassian.net/wiki/display/EN/Electronic+license+keys
                // and bug 1034 in Bugzilla for the reason behind this
                // seemingly weird condition.
                m_ArePartitionsAllowed = (val < 0);
            }
            else
            {
                m_ArePartitionsAllowed = true;
            }
            LOGGER.info("Are partitions allowed = " + m_ArePartitionsAllowed);
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to parse JSON document " + backendInfo, e);
            return;
        }
        catch (ClassCastException e)
        {
            LOGGER.warn("Parsed non-object JSON document " + backendInfo, e);
            return;
        }

        // Try to add extra fields (just appVer for now)
        doc.put(APP_VER_FIELDNAME, apiVersion());

        // Try to persist the modified document
        try
        {
            m_JobProvider.savePrelertInfo(doc.toString());
        }
        catch (Exception e)
        {
            LOGGER.warn("Error writing Prelert info to Elasticsearch", e);
            return;
        }

        LOGGER.info("Wrote Prelert info " + doc.toString() + " to Elasticsearch");
    }

    public String apiVersion()
    {
        // Try to add extra fields (just appVer for now)
        try
        {
            Properties props = new Properties();
            // Try to get the API version as recorded by Maven at build time
            InputStream is = getClass().getResourceAsStream("/META-INF/maven/com.prelert/engine-api/pom.properties");
            if (is != null)
            {
                try
                {
                    props.load(is);
                    return props.getProperty("version");
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

    private static int calculateMaxJobsAllowed()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        double factor = readMaxJobsFactorOrDefault();
        return (int) Math.ceil(cores * factor);
    }

    private static double readMaxJobsFactorOrDefault()
    {
        String readMaxJobsFactor = System.getProperty(MAX_JOBS_FACTOR_NAME);
        if (readMaxJobsFactor == null)
        {
            return DEFAULT_MAX_JOBS_FACTOR;
        }
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
}
