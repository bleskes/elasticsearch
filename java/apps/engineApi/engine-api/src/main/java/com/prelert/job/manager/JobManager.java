/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import com.prelert.job.JobStatus;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.exceptions.JobIdAlreadyExistsException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.none.NoneJobDataPersister;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.process.exceptions.ClosedJobException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.writer.CsvRecordWriter;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.none.NoneStatusReporter;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.data.parsing.AlertObserver;


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
     * The limit on number of partitions per job.
     * A negative limit means unlimited.
     * Note that the Java code can really only do anything with this if it's
     * zero, as it doesn't count the number of distinct values of the partition
     * field.  However, if the limit is zero it can reject any configured
     * partition field settings.
     */
    private int m_MaxPartitionsPerJob = -1;

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
     * Get the details of the specific job wrapped in a <code>SingleDocument</code>
     *
     * @param jobId
     * @return The JobDetails or throws UnknownJobException
     */
    public SingleDocument<JobDetails> getJob(String jobId)
    throws UnknownJobException
    {
        SingleDocument<JobDetails> doc = new SingleDocument<>();
        doc.setType(JobDetails.TYPE);
        doc.setDocumentId(jobId);

        doc.setDocument(m_JobProvider.getJobDetails(jobId));
        doc.setExists(doc.getDocument() != null);

        return doc;
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
    public Pagination<JobDetails> getJobs(int skip, int take)
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
            throw new JobConfigurationException(
                    "Cannot create new job - your license limits you to " +
                    m_MaxDetectorsPerJob +
                    (m_MaxDetectorsPerJob == 1 ? " detector" : " detectors") +
                    " per job, but you have configured " +
                    jobConfig.getAnalysisConfig().getDetectors().size(),
                    ErrorCode.LICENSE_VIOLATION);
        }

        // We can only validate the case of m_MaxPartitionsPerJob being zero in
        // the Java code - anything more subtle has to be left to the C++
        if (m_MaxPartitionsPerJob == 0 && jobConfig.getAnalysisConfig() != null)
        {
            for (com.prelert.job.Detector detector :
                        jobConfig.getAnalysisConfig().getDetectors())
            {
                String partitionFieldName = detector.getPartitionFieldName();
                if (partitionFieldName != null &&
                    partitionFieldName.length() > 0)
                {
                    throw new JobConfigurationException(
                            "Cannot create new job - your license disallows" +
                            " partition fields, but you have configured one.",
                            ErrorCode.LICENSE_VIOLATION);
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
            m_JobProvider.jobIdIsUnique(jobId);
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
            throw new TooManyJobsException(m_LicenseJobLimit,
                    "Cannot create new job - your license limits you to " +
                    m_LicenseJobLimit + " concurrently running " +
                    (m_LicenseJobLimit == 1 ? "job" : "jobs") +
                    ".  You must close a job before you can create a new one.",
                    ErrorCode.LICENSE_VIOLATION);
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
    public SingleDocument<Bucket> bucket(String jobId,
            String bucketId, boolean expand, boolean includeInterim)
    throws NativeProcessRunException, UnknownJobException
    {
        SingleDocument<Bucket> bucket = m_JobProvider.bucket(jobId,
                bucketId, expand, includeInterim);

        if (bucket.isExists() && !expand)
        {
            bucket.getDocument().setRecords(null);
        }

        return bucket;
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
     * @throws NativeProcessRunException
     */
    public Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException, NativeProcessRunException
    {
        Pagination<Bucket> buckets = m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.getDocuments())
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
     * @throws NativeProcessRunException
     */
    public Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take, long startEpochMs, long endBucketMs,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException, NativeProcessRunException
    {
        Pagination<Bucket> buckets =  m_JobProvider.buckets(jobId, expand,
                includeInterim, skip, take, startEpochMs, endBucketMs,
                anomalyScoreThreshold, normalizedProbabilityThreshold);

        if (!expand)
        {
            for (Bucket bucket : buckets.getDocuments())
            {
                bucket.setRecords(null);
            }
        }

        return buckets;
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
    public Pagination<AnomalyRecord> records(String jobId,
            int skip, int take, boolean includeInterim, String sortField, boolean sortDescending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws NativeProcessRunException, UnknownJobException
    {
        return m_JobProvider.records(jobId,
                skip, take, includeInterim, sortField, sortDescending,
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
    public Pagination<AnomalyRecord> records(String jobId,
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

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param calcInterim Should interim results be calculated based on the data
     * up to the point of the flush?
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if a data upload is part way through
     */
    public void flushJob(String jobId, boolean calcInterim)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Flush job " + jobId);

        // First check the job is in the database.
        // this method throws if it isn't
        if (m_JobProvider.jobExists(jobId))
        {
            m_ProcessManager.flushJob(jobId, calcInterim);
        }
    }

    /**
     * Stop the running job and mark it as finished.<br/>
     *
     * @param jobId The job to stop
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if the job cannot be closed because data is
     * being streamed to it
     */
    public void finishJob(String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Finish job " + jobId);

        // First check the job is in the database.
        // this method throws if it isn't
        if (m_JobProvider.jobExists(jobId))
        {
            m_ProcessManager.finishJob(jobId);
        }
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
     * @throws NativeProcessRunException
     * @throws JobInUseException If the job cannot be deleted because the
     * native process is in use.
     */
    public boolean deleteJob(String jobId)
    throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Deleting job '" + jobId + "'");

        if (m_ProcessManager.jobIsRunning(jobId))
        {
            m_ProcessManager.finishJob(jobId);
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
    public DataCounts submitDataLoadJob(String jobId, InputStream input)
    throws UnknownJobException, NativeProcessRunException, MissingFieldException,
        JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
        OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        return submitDataLoadJob(jobId, input, false);
    }

    /**
     * Passes data to the native process and requires its persistence.
     * If the process is not running a new one is started.
     *
     * This is a blocking call that won't return until all the data has been
     * written to the process. A new thread is launched to parse the process's
     * output
     *
     * @param jobId
     * @param input
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
    public DataCounts submitDataLoadAndPersistJob(String jobId, InputStream input)
    throws UnknownJobException, NativeProcessRunException, MissingFieldException,
        JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
        OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        return submitDataLoadJob(jobId, input, true);
    }

    private  DataCounts submitDataLoadJob(String jobId, InputStream input, boolean shouldPersist)
            throws UnknownJobException, NativeProcessRunException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        checkTooManyJobs(jobId);
        DataCounts stats = tryProcessingDataLoadJob(jobId, input, shouldPersist);
        updateLastDataTime(jobId, new Date());

        return stats;
    }


    private DataCounts tryProcessingDataLoadJob(String jobId, InputStream input, boolean shouldPersist)
            throws UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            NativeProcessRunException, MalformedJsonException
    {
        DataCounts stats;
        try
        {
            stats = shouldPersist ? m_ProcessManager.processDataLoadAndPersistJob(jobId, input)
                    : m_ProcessManager.processDataLoadJob(jobId, input);
        }
        catch (NativeProcessRunException ne)
        {
            tryFinishingJob(jobId);

            //rethrow
            throw ne;
        }
        return stats;
    }

    public String preveiwTransforms(String jobId, InputStream input)
    throws JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
    OutOfOrderRecordsException, MalformedJsonException, IOException, UnknownJobException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CsvRecordWriter writer = new CsvRecordWriter(output);
        JobDetails job = m_JobProvider.getJobDetails(jobId);

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
            throw new TooManyJobsException(m_MaxAllowedJobs,
                    "Cannot reactivate job with id '" + jobId +
                    "'. The maximum numuber of concurrently running jobs is limited as a function " +
                    "of the number of CPU cores see this error code's help documentation " +
                    "for details of how to elevate the setting",
                    ErrorCode.TOO_MANY_JOBS_RUNNING_CONCURRENTLY);
        }
    }

    private void checkDataLoadForTooManyJobsAgainstLicenseLimit(String jobId)
            throws TooManyJobsException
    {
        if (areMoreJobsRunningThanLicenseLimit())
        {
            throw new TooManyJobsException(m_LicenseJobLimit,
                    "Cannot reactivate job with id '" + jobId +
                    "' - your license limits you to " + m_LicenseJobLimit +
                    " concurrently running jobs. You must close a job before" +
                    " you can reactivate a closed one.",
                    ErrorCode.LICENSE_VIOLATION);
        }
    }


    private void tryFinishingJob(String jobId) throws JobInUseException
    {
        try
        {
            m_ProcessManager.finishJob(jobId);
        }
        catch (NativeProcessRunException e)
        {
            LOGGER.warn("Error finishing job after submitDataLoadJob failed", e);
        }
    }

    /**
     * The job id is a concatenation of the date in 'yyyyMMddHHmmss' format
     * and a sequence number that is a minimum of 5 digits wide left padded
     * with zeros.<br/>
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
        try
        {
            return m_JobProvider.getJobDetails(refId);
        }
        catch (UnknownJobException e)
        {
            throw new UnknownJobException(refId, "Missing Job: Cannot find "
                    + "referenced job with id '" + refId + "'",
                    ErrorCode.UNKNOWN_JOB_REFERENCE);

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
                m_MaxPartitionsPerJob = constraint.asInt(-1);
            }
            else
            {
                m_MaxPartitionsPerJob = -1;
            }
            LOGGER.info("Max partitions per job = " + m_MaxPartitionsPerJob);
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
