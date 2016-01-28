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

package com.prelert.rs.client.integrationtests;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * Test the Engine REST API endpoints.
 * Creates jobs, uploads data closes the jobs then gets the results.
 * Tests all the API endpoints and query parameters
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing these 3 files:
 * <ol>
 * <li>/engine_api_integration_test/flightcentre.csv.gz</li>
 * <li>/engine_api_integration_test/flightcentre.json</li>
 * <li>/engine_api_integration_test/farequote_ISO_8601.csv</li>
 * </ol>
 *
 * These tests will only pass if the Engine has a full license.
 * The developer license does not allow > 1 job
 *
 */
public class JobsTest implements Closeable
{
    static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    static final String ISO_8601_DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";


    static final long FLIGHT_CENTRE_NUM_BUCKETS = 24;
    /**
     *  The number of input records in the farequote input data set
     *  excluding those in the final bucket
     */
    static final long FLIGHT_CENTRE_NUM_EVENTS = 167517;

    static final long FARE_QUOTE_NUM_BUCKETS = 1439;
    /**
     * The number of records in the farequote input data
     * excluding those in the final bucket
     */
    static final long FARE_QUOTE_NUM_EVENTS = 86229;


    private static final Logger LOGGER = Logger.getLogger(JobsTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final String m_BaseUrl;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public JobsTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_BaseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    /**
     * Get all the jobs and test the pagination objects values are
     * set correctly
     *
     * @return
     * @throws IOException
     */
    public boolean getJobsTest()
    throws IOException
    {
        Pagination<JobDetails> jobs = m_WebServiceClient.getJobs();

        test(jobs.getHitCount() >= jobs.getDocuments().size());
        test(jobs.getTake() > 0);
        if (jobs.getHitCount() < jobs.getTake())
        {
            test(jobs.getNextPage() == null);
            test(jobs.getPreviousPage() == null);
        }
        else
        {
            String prevPageUrl = null;
            if (jobs.getSkip() > 0)
            {
                int start = Math.max(0,  jobs.getSkip() -jobs.getTake());
                prevPageUrl = String.format("%s?skip=%d&take=%d", m_BaseUrl,
                        start, jobs.getTake());
            }
            test((prevPageUrl == null && jobs.getPreviousPage() == null) ||
                    prevPageUrl.equals(jobs.getPreviousPage().toString()));

            String nextPageUrl = null;
            if (jobs.getHitCount() > jobs.getSkip() + jobs.getTake())
            {
                int start = jobs.getSkip() + jobs.getTake();
                nextPageUrl = String.format("%s?skip=%d&take=%d", m_BaseUrl,
                        start, jobs.getTake());
            }
            test((nextPageUrl == null && jobs.getNextPage() == null) ||
                    nextPageUrl.equals(jobs.getNextPage().toString()));
        }

        // jobs should be sorted by ascending Id
        if (jobs.getDocuments().size() > 1)
        {
            String lastId = jobs.getDocuments().get(0).getId();
            for (int i=1; i<jobs.getDocuments().size(); i++)
            {
                String currentId = jobs.getDocuments().get(i).getId();

                test(lastId.compareTo(currentId) < 0);

                lastId = jobs.getDocuments().get(i).getId();
            }
        }

        return true;
    }


    /**
     * Creates a job using the FlightCentre configuration then
     * reads it back verifying all the correct properties are set.
     *
     * @return The Id of the created job
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String createFlightCentreCsvJobTest()
    throws ClientProtocolException, IOException
    {
        final String FLIGHT_CENTRE_JOB_CONFIG = "{\"id\":\"flightcentre-csv\","
                + "\"description\":\"Flight Centre Job\","
                + "\"analysisConfig\" : {"
                + "\"bucketSpan\":3600,"
                //+ "\"detectors\":[{\"function\":\"count\"}, {\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
                + "},"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", \"timeFormat\" : \"epoch\"},"
                + "\"analysisLimits\": {\"modelMemoryLimit\":2000}"
                + "}";


        String jobId = m_WebServiceClient.createJob(FLIGHT_CENTRE_JOB_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals("flightcentre-csv"));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        Detector d = new Detector();
        d.setDetectorDescription("responsetime by airline");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");


        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));

        AnalysisLimits al = new AnalysisLimits(2000, null);
        test(job.getAnalysisLimits().equals(al));

        test(job.getDescription().equals("Flight Centre Job"));

        test(job.getLocation().toString().equals(m_BaseUrl + "/jobs/" + jobId));
        test(job.getRecordsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/records"));
        test(job.getBucketsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/buckets"));
        test(job.getDataEndpoint().toString().equals(m_BaseUrl + "/data/" + jobId));
        test(job.getCategoryDefinitionsEndpoint().toString().equals(m_BaseUrl + "/results/"+ jobId + "/categorydefinitions"));
        test(job.getAlertsLongPollEndpoint().toString().equals(m_BaseUrl + "/alerts_longpoll/" + jobId));

        test(job.getLastDataTime() == null);
        test(job.getFinishedTime() == null);

        test(job.getStatus() == JobStatus.CLOSED);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -2);
        Date twoMinsAgo = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        Date twoMinsInFuture = cal.getTime();

        test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));

        return jobId;
    }

    /**
     * Creates a job using the Farequote ISO 8601 time format configuration
     * then reads it back verifying all the correct properties are set.
     *
     * @return The Id of the created job
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String createFareQuoteTimeFormatJobTest()
    throws ClientProtocolException, IOException
    {
        final String FARE_QUOTE_TIME_FORMAT_CONFIG = "{"
                + "\"description\":\"Farequote Time Format Job\","
                + "\"analysisConfig\" : {"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
                + "},"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"time\", "
                + "\"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"} }}";

        String jobId = m_WebServiceClient.createJob(FARE_QUOTE_TIME_FORMAT_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            test(jobId != null);
        }

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        verifyFareQuoteTimeFormatJobTest(job, jobId);

        return jobId;
    }


    /**
     * Creates a job for the flightcentre csv data with the date in ms
     * from the epoch
     *
     * @param baseUrl The URL of the REST API i.e. an URL like
     *     <code>http://prelert-host:8080/engine/version/</code>
     * @param name The name for the job
     *
     * @return The Id of the created job
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String createFlightCentreMsCsvFormatJobTest(String baseUrl, String name)
    throws ClientProtocolException, IOException
    {
        Detector d = new Detector();
        d.setDetectorDescription("responsetime by airline partitionfield=sourcetype");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");
        dd.setTimeFormat("epoch_ms");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(name);
        config.setDataDescription(dd);


        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null)
        {
            LOGGER.error("No Job Id returned by create job");
            test(false);
        }
        test(jobId.equals(name));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();


        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));
        test(job.getAnalysisLimits() == null);

        test(job.getId().equals(name));
        test(job.getDescription() == null);

        test(job.getLocation().toString().equals(baseUrl + "/jobs/" + jobId));
        test(job.getRecordsEndpoint().toString().equals(baseUrl + "/results/" + jobId + "/records"));
        test(job.getBucketsEndpoint().toString().equals(baseUrl + "/results/" + jobId + "/buckets"));
        test(job.getDataEndpoint().toString().equals(baseUrl + "/data/" + jobId));

        test(job.getLastDataTime() == null);
        test(job.getFinishedTime() == null);

        test(job.getStatus() == JobStatus.CLOSED);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -2);
        Date twoMinsAgo = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        Date twoMinsInFuture = cal.getTime();

        test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));

        return jobId;
    }


    /**
     * Creates a job for the flightcentre JSON data with the date in ms
     * from the epoch
     *
     * @return The Id of the created job
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String createFlightCentreMsJsonFormatJobTest()
    throws ClientProtocolException, IOException
    {
        Detector d = new Detector();
        d.setDetectorDescription("responsetime by airline");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");
        dd.setTimeFormat("epoch_ms");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDataDescription(dd);


        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null)
        {
            LOGGER.error("No Job Id returned by create job");
            test(false);
        }

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();


        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));
        test(job.getAnalysisLimits() == null);

        test(job.getLocation().toString().equals(m_BaseUrl + "/jobs/" + jobId));
        test(job.getRecordsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/records"));
        test(job.getBucketsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/buckets"));
        test(job.getDataEndpoint().toString().equals(m_BaseUrl + "/data/" + jobId));

        test(job.getLastDataTime() == null);
        test(job.getFinishedTime() == null);

        test(job.getDescription() == null);

        test(job.getStatus() == JobStatus.CLOSED);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -2);
        Date twoMinsAgo = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        Date twoMinsInFuture = cal.getTime();

        test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));


        return jobId;
    }

    private void verifyFareQuoteTimeFormatJobTest(JobDetails job, String jobId)
    {
        Detector d = new Detector();
        d.setDetectorDescription("responsetime by airline");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));
        test(job.getAnalysisLimits() == null);

        test(job.getDescription().equals("Farequote Time Format Job"));

        test(job.getLocation().toString().equals(m_BaseUrl + "/jobs/" + jobId));
        test(job.getRecordsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/records"));
        test(job.getBucketsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/buckets"));
        test(job.getDataEndpoint().toString().equals(m_BaseUrl + "/data/" + jobId));

        test(job.getLastDataTime() == null);
        test(job.getFinishedTime() == null);

        test(job.getStatus() == JobStatus.CLOSED);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -2);
        Date twoMinsAgo = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        Date twoMinsInFuture = cal.getTime();

        test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));
    }

    /**
     * Creates a job using the FlightCentre Json configuration then
     * reads it back verifying all the correct properties are set.
     *
     * @return The Id of the created job
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String createFlightCentreJsonJobTest()
    throws ClientProtocolException, IOException
    {
        Detector d = new Detector();
        d.setDetectorDescription("FlightCentre analysis");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");

        JobConfiguration jobConfig = new JobConfiguration(ac);
        jobConfig.setDataDescription(dd);
        jobConfig.setDescription("Flight Centre JSON");

        String jobId = m_WebServiceClient.createJob(jobConfig);
        if (jobId == null)
        {
            LOGGER.error("No Job Id returned by create job");
            test(false);
        }

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));
        test(job.getAnalysisLimits() == null);

        test(job.getLocation().toString().equals(m_BaseUrl + "/jobs/" + jobId));
        test(job.getRecordsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/records"));
        test(job.getBucketsEndpoint().toString().equals(m_BaseUrl + "/results/" + jobId + "/buckets"));
        test(job.getDataEndpoint().toString().equals(m_BaseUrl + "/data/" + jobId));

        test(job.getLastDataTime() == null);
        test(job.getFinishedTime() == null);

        test(job.getStatus() == JobStatus.CLOSED);

        test(job.getDescription().equals("Flight Centre JSON"));

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -2);
        Date twoMinsAgo = cal.getTime();
        cal.add(Calendar.MINUTE, 4);
        Date twoMinsInFuture = cal.getTime();

        test(job.getCreateTime().after(twoMinsAgo) && job.getCreateTime().before(twoMinsInFuture));

        return jobId;
    }

    /**
     * Slowly upload the contents of <code>dataFile</code> 1024 bytes at a
     * time to the server. Starts a background thread to write the data
     * and sleeps between each 1024B upload.
     * </br>
     * This is to show that a slow streaming server works the
     * same as a faster local server.
     *
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param sleepTimeMs The duration of the sleep in milliseconds
     * @throws IOException
     */
    public void slowUpload(String jobId, final File dataFile, final long sleepTimeMs)
    throws IOException
    {
        final PipedInputStream pipedIn = new PipedInputStream();
        final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);

        new Thread(new Runnable() {
            @Override
            public void run() {
                int n;
                byte [] buf = new byte[2048];
                try
                {
                    FileInputStream fs;
                    try
                    {
                        fs = new FileInputStream(dataFile);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                        return;
                    }

                    while((n = fs.read(buf)) > -1)
                    {
                        pipedOut.write(buf, 0, n);
                        Thread.sleep(sleepTimeMs);
                    }
                    fs.close();

                    pipedOut.close();
                }
                catch (IOException e)
                {
                    LOGGER.info(e);
                }
                catch (InterruptedException e)
                {
                    LOGGER.info(e);
                }
            }
        }).start();

        m_WebServiceClient.streamingUpload(jobId, pipedIn, false);
    }


    /**
     * Upload the contents of <code>dataFile</code> to the server.
     *
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param compressed Is the data gzipped compressed?
     * @throws IOException
     */
    public void uploadDataAndTestRecordsWereProcessed(String jobId, File dataFile, boolean compressed)
    throws IOException
    {
        uploadData(jobId, dataFile, compressed, true);
    }

    public void uploadDataAndTestNoRecordsWereProcessed(String jobId, File dataFile, boolean compressed)
    throws IOException
    {
        uploadData(jobId, dataFile, compressed, false);
    }

    private void uploadData(String jobId, File dataFile, boolean compressed,
            boolean shouldProcessRecords) throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);
        MultiDataPostResult result = m_WebServiceClient.streamingUpload(jobId, stream, compressed);

        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();

        if (shouldProcessRecords)
        {
            test(error == null);
            test(result.anErrorOccurred() == false);
            test(result.getResponses().get(0).getUploadSummary().getProcessedRecordCount() > 0);
        }
        else
        {
            test(error != null);
            test(result.anErrorOccurred() == true);
            test(result.getResponses().get(0).getUploadSummary() == null);
        }

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.RUNNING);
    }

    /**
     * Finish the job (as all data has been uploaded).
     *
     * @param jobId The Job's Id
     * @return
     * @throws IOException
     */
    public boolean closeJob(String jobId)
    throws IOException
    {
        boolean closed = m_WebServiceClient.closeJob(jobId);
        test(closed);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.CLOSED);

        return closed;
    }

    /**
     * Read the job bucket results and verify they are present and have
     * sensible values then do the same again but getting the anomaly
     * records inline using the <code>expand</code> query parameter.
     *
     * @param jobId The job id
     * @param take The max number of buckets to return
     * @param expectedNumBuckets The expected number of result buckets in the job
     * @param bucketSpan Bucket span in seconds
     * @param expectedTotalEvents The total number of input records (events)
     * expected for the job
     *
     * @throws IOException
     */
    public void verifyJobResults(String jobId, long take,
            long expectedNumBuckets, long bucketSpan, long expectedTotalEvents)
    throws IOException
    {
        LOGGER.debug("Verifying results for job " + jobId);

        long skip = 0;
        long lastBucketTime = 0;
        long eventCount = 0;
        while (true) // break when getNextUrl() == false
        {
            Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                    .skip(skip).take(take).get();

            test(buckets.getHitCount() == expectedNumBuckets);
            test(buckets.getDocumentCount() <= take);
            validateBuckets(buckets.getDocuments(), bucketSpan, lastBucketTime, false);

            for (Bucket b: buckets.getDocuments())
            {
                eventCount += b.getEventCount();
            }

            // time in seconds
            lastBucketTime = buckets.getDocuments().get(
                    buckets.getDocuments().size() -1).getTimestamp().getTime() / 1000;

            SingleDocument<Bucket> bucket = m_WebServiceClient.prepareGetBucket(jobId,
                    Long.toString(lastBucketTime)).get();

            test(bucket.isExists() == true);
            test(bucket.getDocument() != null);
            test(bucket.getDocumentId().equals(Long.toString(lastBucketTime)));
            test(bucket.getType().equals(Bucket.TYPE));

            validateBuckets(Arrays.asList(new Bucket[]{bucket.getDocument()}),
                    bucketSpan, 0, false);

            SingleDocument<Bucket> nonExistentBucket = m_WebServiceClient.prepareGetBucket(jobId,
                    "missing_bucket").get();
            test(nonExistentBucket.isExists() == false);
            test(nonExistentBucket.getDocument() == null);
            test(nonExistentBucket.getDocumentId().equals("missing_bucket"));
            test(nonExistentBucket.getType().equals(Bucket.TYPE));


            if (skip > 0)
            {
                // should be a previous page
                test(buckets.getPreviousPage() != null);

                int start = Math.max(0,  buckets.getSkip() - buckets.getTake());
                String prevPageUrl = String.format(
                        "%s/results/%s/buckets?skip=%d&take=%d&expand=%b&includeInterim=%b&anomalyScore=0.0&maxNormalizedProbability=0.0",
                        m_BaseUrl, jobId,  start, buckets.getTake(), false, false);

                test(prevPageUrl.equals(buckets.getPreviousPage().toString()));
            }


            if (buckets.getNextPage() == null)
            {
                test(expectedNumBuckets == (skip + buckets.getDocumentCount()));
                break;
            }
            else
            {
                int start = Math.max(0,  buckets.getSkip() + buckets.getTake());
                String nextPageUrl = String.format(
                        "%s/results/%s/buckets?skip=%d&take=%d&expand=%b&includeInterim=%b&anomalyScore=0.0&maxNormalizedProbability=0.0",
                        m_BaseUrl, jobId, start, buckets.getTake(), false, false);

                test(nextPageUrl.equals(buckets.getNextPage().toString()));
            }


            skip += take;
        }

        test(eventCount == expectedTotalEvents);

        // the same with expanded buckets
        skip = 0;
        lastBucketTime = 0;
        eventCount = 0;
        while (true) // break when getNextUrl() == false
        {
            Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                    .expand(true).skip(skip).take(take).get();

            test(buckets.getHitCount() == expectedNumBuckets);
            test(buckets.getDocumentCount() <= take);
            validateBuckets(buckets.getDocuments(), bucketSpan, lastBucketTime, true);

            for (Bucket b: buckets.getDocuments())
            {
                eventCount += b.getEventCount();
            }

            // time in seconds
            lastBucketTime = buckets.getDocuments().get(
                    buckets.getDocuments().size() -1).getTimestamp().getTime() / 1000;

            SingleDocument<Bucket> bucket = m_WebServiceClient
                    .prepareGetBucket(jobId, Long.toString(lastBucketTime)).expand(true).get();

            test(bucket.isExists() == true);
            test(bucket.getDocument() != null);
            test(bucket.getDocumentId().equals(Long.toString(lastBucketTime)));
            test(bucket.getType().equals(Bucket.TYPE));

            validateBuckets(Arrays.asList(new Bucket[]{bucket.getDocument()}),
                    bucketSpan, 0, true);

            SingleDocument<Bucket> nonExistentBucket = m_WebServiceClient.prepareGetBucket(jobId,
                    "missing_bucket").get();
            test(nonExistentBucket.isExists() == false);
            test(nonExistentBucket.getDocument() == null);
            test(nonExistentBucket.getDocumentId().equals("missing_bucket"));
            test(nonExistentBucket.getType().equals(Bucket.TYPE));


            if (skip > 0)
            {
                // should be a previous page
                test(buckets.getPreviousPage() != null);

                int start = Math.max(0,  buckets.getSkip() - buckets.getTake());
                String prevPageUrl = String.format(
                        "%s/results/%s/buckets?skip=%d&take=%d&expand=%b&includeInterim=%b&anomalyScore=0.0&maxNormalizedProbability=0.0",
                        m_BaseUrl, jobId, start, buckets.getTake(), true, false);

                test(prevPageUrl.equals(buckets.getPreviousPage().toString()));
            }

            if (buckets.getNextPage() == null)
            {
                test(expectedNumBuckets == (skip + buckets.getDocumentCount()));
                break;
            }
            else
            {
                int start = Math.max(0,  buckets.getSkip() + buckets.getTake());
                String nextPageUrl = String.format(
                        "%s/results/%s/buckets?skip=%d&take=%d&expand=%b&includeInterim=%b&anomalyScore=0.0&maxNormalizedProbability=0.0",
                        m_BaseUrl, jobId, start, buckets.getTake(), true, false);

                test(nextPageUrl.equals(buckets.getNextPage().toString()));
            }

            skip += take;
        }

        test(eventCount == expectedTotalEvents);
    }


    /**
     * Simple verification that the buckets have sensible values
     *
     * @param buckets
     * @param bucketSpan
     * @param lastBucketTime The first bucket in this list should be at time
     * <code>lastBucketTime + bucketSpan</code> unless this value is 0
     * in which case the bucket is the first ever.
     * @param expanded
     */
    private void validateBuckets(List<Bucket> buckets, long bucketSpan,
            long lastBucketTime, boolean expanded)
    {
        test(buckets.size() > 0);


        for (Bucket b : buckets)
        {
            test(b.getAnomalyScore() >= 0.0);
            test(b.getRecordCount() >= 0);
            if (b.getRecordCount() == 0)
            {
                test(b.getBucketInfluencers().size() == 0);
            }
            else
            {
                test(b.getBucketInfluencers().size() == 1);
                test(b.getBucketInfluencers().get(0).getAnomalyScore() == b.getAnomalyScore());
                test(b.getBucketInfluencers().get(0).getProbability() >= 0.0);
                test(b.getBucketInfluencers().get(0).getProbability() <= 1.0);
            }
            test(b.getId() != null && b.getId().isEmpty() == false);
            long epoch = b.getEpoch();
            Date date = new Date(epoch * 1000);

            // sanity check, the data may be old but it should be newer than 2010
            final long firstJan2010 = 1262304000000L;
            test(date.after(new Date(firstJan2010)));
            test(b.getTimestamp().after(new Date(firstJan2010)));
            // data shouldn't be newer than now
            test(b.getTimestamp().before(new Date()));

            // epoch and timestamp should be the same
            test(date.equals(b.getTimestamp()));

            // must be more than 0 events
            test(b.getEventCount() > 0);

            if (lastBucketTime > 0)
            {
                lastBucketTime += bucketSpan;
                test(epoch == lastBucketTime);
            }


            if (expanded)
            {
                test(b.getRecords().size() >= 0);
                test(b.getRecordCount() >= 0);
                test(b.getRecordCount() == b.getRecords().size());

                // records are sorted by probability ascending
                double probability = 0.0;
                for (AnomalyRecord r: b.getRecords())
                {
                    test(r.getProbability() >= probability);
                    probability = r.getProbability();

                    // at a minimum all records should have this field
                    test(r.getFunction() != null);
                }
            }
            else
            {
                test(b.getRecords() == null || b.getRecords().size() == 0);
            }
        }

    }


    /**
     * Test filtering bucket results by date.
     * Tests each of the 3 acceptable date formats and paging the results.
     *
     * @param jobId The job id
     * @param start Filter start date
     * @param end Filter end date
     *
     * @throws IOException
     */
    public void testBucketDateFilters(String jobId, Date start, Date end)
    throws IOException
    {
        // test 3 date formats
        Long epochStart = start.getTime() / 1000;
        Long epochEnd = end.getTime() / 1000;
        String dateStart = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(start);
        String dateEnd = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(end);
        String dateStartMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(start);
        String dateEndMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(end);

        // query with the 3 date formats
        Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                .start(epochStart).end(epochEnd).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
        test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
                .getTimestamp().compareTo(end) < 0);

        buckets = m_WebServiceClient.prepareGetBuckets(jobId).start(dateStart).end(dateEnd).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
        test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
                .getTimestamp().compareTo(end) < 0);

        buckets = m_WebServiceClient.prepareGetBuckets(jobId).start(dateStartMs).end(dateEndMs).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);
        test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
                .getTimestamp().compareTo(end) < 0);


        // just a start date
        buckets = m_WebServiceClient.prepareGetBuckets(jobId).start(dateStart).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);

        buckets = m_WebServiceClient.prepareGetBuckets(jobId).start(epochStart).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);


        // just an end date
        buckets = m_WebServiceClient.prepareGetBuckets(jobId).end(dateEndMs).get();
        test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
                .getTimestamp().compareTo(end) < 0);

        buckets = m_WebServiceClient.prepareGetBuckets(jobId).end(dateEnd).get();
        test(buckets.getDocuments().get(buckets.getDocumentCount() -1)
                .getTimestamp().compareTo(end) < 0);


        // Test paging from the start date

        buckets = m_WebServiceClient.prepareGetBuckets(jobId).skip(0).take(5).start(dateStart).get();
        test(buckets.getDocuments().get(0).getTimestamp().compareTo(start) >= 0);

        Date lastDate = buckets.getDocuments().get(buckets.getDocumentCount() -1).getTimestamp();

        int bucketCount = 0;
        while (buckets.getNextPage() != null)
        {
            String url = buckets.getNextPage().toString();
            buckets = m_WebServiceClient.<Pagination<Bucket>>get(url,
                        new TypeReference<Pagination<Bucket>>() {});

            Date firstDate = buckets.getDocuments().get(0).getTimestamp();

            test(firstDate.compareTo(lastDate) >= 0);

            lastDate = buckets.getDocuments().get(buckets.getDocumentCount() -1)
                    .getTimestamp();
            bucketCount++;
        }

        // and page backwards
        while (buckets.getPreviousPage() != null)
        {
            String url = buckets.getPreviousPage().toString();
            buckets = m_WebServiceClient.<Pagination<Bucket>>get(url,
                        new TypeReference<Pagination<Bucket>>() {});

            Date date = buckets.getDocuments().get(buckets.getDocumentCount() -1)
                    .getTimestamp();

            test(date.compareTo(lastDate) <= 0);

            lastDate = buckets.getDocuments().get(0).getTimestamp();
            bucketCount--;
        }

        test(bucketCount == 0);
    }

    /**
     * Get buckets filtered by anomaly & unusual scores.
     *
     * @param jobId
     * @throws IOException
     */
    public void testBucketScoreFilters(String jobId)
    throws IOException
    {
        Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                        .take(4000)
                        .anomalyScoreThreshold(50.0)
                        .normalizedProbabilityThreshold(40.0).get();
        test(buckets.getDocumentCount() > 0);

        for (Bucket b : buckets.getDocuments())
        {
            test(b.getAnomalyScore() >= 50.0);
            test(b.getMaxNormalizedProbability()  >= 40.0);
        }

        buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                .take(4000)
                .anomalyScoreThreshold(20.0)
                .normalizedProbabilityThreshold(0.0).get();
        test(buckets.getDocumentCount() > 0);

        for (Bucket b : buckets.getDocuments())
        {
            test(b.getAnomalyScore() >= 20.0);
        }

        buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                .take(4000)
                .anomalyScoreThreshold(20.0)
                .normalizedProbabilityThreshold(15.0).get();
        test(buckets.getDocumentCount() > 0);

        for (Bucket b : buckets.getDocuments())
        {
            test(b.getMaxNormalizedProbability() >= 15.0);
        }
    }


    /**
     * Test the sort options for the records endpoint.
     * Sort by anomaly score, unsual score, time, etc
     *
     * @param jobId The job id
     * @param start Filter start date
     * @param end Filter end date
     * @throws IOException
     */
    public void testSortingRecords(String jobId, Date start, Date end)
    throws IOException
    {
        // test 3 date formats
        Long epochStart = start.getTime() / 1000;
        Long epochEnd = end.getTime() / 1000;
        String dateStartMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(start);
        String dateEnd = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(end);

        // most unusual first

        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(500)
                        .start(epochStart)
                        .end(epochEnd)
                        .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
                        .descending(true).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        double score = 100.0; // max score
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getNormalizedProbability() <= score);
            score = r.getNormalizedProbability();
        }

        // least unusual first
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(500)
                        .start(epochStart)
                        .end(epochEnd)
                        .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
                        .descending(false).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        score = 0.0;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getNormalizedProbability() >= score);
            score = r.getNormalizedProbability();
        }

        // most anomalous first
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .start(epochStart)
                        .end(epochEnd)
                        .sortField(AnomalyRecord.ANOMALY_SCORE)
                        .descending(true).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        score = 100.0;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getAnomalyScore() <= score);
            score = r.getAnomalyScore();
        }

        // lease anomalous first
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .start(epochStart)
                        .end(epochEnd)
                        .sortField(AnomalyRecord.ANOMALY_SCORE)
                        .descending(false).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        score = 0.0;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getAnomalyScore() >= score);
            score = r.getAnomalyScore();
        }


        // order by by-field value
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(500)
                        .start(dateStartMs)
                        .end(dateEnd)
                        .sortField("byFieldValue")
                        .descending(true).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        String fieldValue = "ZZZZZZZZZZZZZZZZ";
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getByFieldValue().compareTo(fieldValue) <= 0);
            fieldValue = r.getByFieldValue();
        }


        // order by time oldest first
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(500)
                        .start(epochStart)
                        .end(epochEnd)
                        .sortField("timestamp")
                        .descending(false).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        long startTime = 0l;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getTimestamp().getTime() >= startTime);
            startTime = r.getTimestamp().getTime();
        }

        // order by time newest first
        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(500)
                        .start(dateStartMs)
                        .end(dateEnd)
                        .sortField("timestamp")
                        .descending(true).get();

        test(records.getDocumentCount() > 0);
        test(records.getDocumentCount() == records.getDocuments().size());

        startTime = new Date().getTime();
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getTimestamp().getTime() <= startTime);
            startTime = r.getTimestamp().getTime();
        }
    }

    /**
     * Filter records by anomaly score or unusual score.
     * Checks that the returned records all have a score >= the filter value
     *
     * @param jobId
     * @throws IOException
     */
    public void testRecordScoreFilters(String jobId)
    throws IOException
    {
        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(4000)
                        .sortField(AnomalyRecord.ANOMALY_SCORE)
                        .descending(true)
                        .anomalyScoreThreshold(8.0)
                        .normalizedProbabilityThreshold(20.0).get();

        test(records.getDocumentCount() > 0);
        double score = 100.0;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getAnomalyScore() >= 8.0);
            test(r.getNormalizedProbability() >= 20.0);

            test(r.getAnomalyScore() <= score);
            score = r.getAnomalyScore();
        }

        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(4000)
                        .sortField(AnomalyRecord.BY_FIELD_VALUE)
                        .descending(false)
                        .normalizedProbabilityThreshold(12.5).get();

        test(records.getDocumentCount() > 0);

        String fieldValue = "";
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getNormalizedProbability() >= 12.5);

            test(r.getByFieldValue().compareTo(fieldValue) >= 0);
            fieldValue = r.getByFieldValue();
        }

        records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(4000)
                        .sortField(AnomalyRecord.ANOMALY_SCORE)
                        .descending(true)
                        .anomalyScoreThreshold(40.0)
                        .normalizedProbabilityThreshold(0.0).get();

        test(records.getDocumentCount() > 0);

        score = 100.0;
        for (AnomalyRecord r : records.getDocuments())
        {
            test(r.getAnomalyScore() >= 40.0);

            test(r.getAnomalyScore() <= score);
            score = r.getAnomalyScore();
        }
    }


    /**
     * Test filtering bucket results by date.
     * Tests each of the 3 acceptable date formats and paging the results.
     *
     * @param jobId The job id
     * @param start Filter start date
     * @param end Filter end date
     *
     * @throws IOException
     */
    public void testRecordDateFilters(String jobId, Date start, Date end)
    throws IOException
    {

        // test 3 date formats
        Long epochStart = start.getTime() / 1000;
        Long epochEnd = end.getTime() / 1000;
        String dateStart = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(start);
        String dateEnd = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(end);
        String dateStartMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(start);
        String dateEndMs = new SimpleDateFormat(ISO_8601_DATE_FORMAT_WITH_MS).format(end);

        // query with the 3 date formats
        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(jobId)
                .start(epochStart).end(epochEnd).get();
        testBetweeen2Dates(records.getDocuments(), start, end);

        records = m_WebServiceClient.prepareGetRecords(jobId).start(dateStart).end(dateEnd).get();
        testBetweeen2Dates(records.getDocuments(), start, end);

        records = m_WebServiceClient.prepareGetRecords(jobId).start(dateStartMs).end(dateEndMs).get();
        testBetweeen2Dates(records.getDocuments(), start, end);


        // just a start date
        records = m_WebServiceClient.prepareGetRecords(jobId).start(dateStart).get();
        testBetweeen2Dates(records.getDocuments(), start, new Date());

        records = m_WebServiceClient.prepareGetRecords(jobId).start(epochStart).get();
        testBetweeen2Dates(records.getDocuments(), start, new Date());


        // just an end date
        records = m_WebServiceClient.prepareGetRecords(jobId).end(dateEndMs).get();
        testBetweeen2Dates(records.getDocuments(), new Date(0), end);

        records = m_WebServiceClient.prepareGetRecords(jobId).end(dateEnd).get();
        testBetweeen2Dates(records.getDocuments(), new Date(0), end);


        // Test paging from the start date
        records = m_WebServiceClient.prepareGetRecords(jobId).take(5).start(dateStart).get();
        testBetweeen2Dates(records.getDocuments(), start, new Date());


        int bucketCount = 0;
        while (records.getNextPage() != null)
        {
            String url = records.getNextPage().toString();
            records = m_WebServiceClient.<Pagination<AnomalyRecord>>get(url,
                        new TypeReference<Pagination<AnomalyRecord>>() {});

            testBetweeen2Dates(records.getDocuments(), start, new Date());
            bucketCount++;
        }

        // and page backwards
        while (records.getPreviousPage() != null)
        {
            String url = records.getPreviousPage().toString();
            records = m_WebServiceClient.<Pagination<AnomalyRecord>>get(url,
                        new TypeReference<Pagination<AnomalyRecord>>() {});

            testBetweeen2Dates(records.getDocuments(), start, new Date());
            bucketCount--;
        }

        test(bucketCount == 0);
    }


    private void testBetweeen2Dates(List<AnomalyRecord> records, Date start, Date end)
    {
        test(records.size() > 0);
        for (AnomalyRecord r : records)
        {
            test(r.getTimestamp().compareTo(start) >= 0);
            test(r.getTimestamp().compareTo(end) < 0);
        }
    }


    /**
     * Test setting the description field of the job
     *
     * @param jobId The job id
     * @throws IOException
     */
    public void testSetDescription(String jobId)
    throws IOException
    {
        JobDetails job = m_WebServiceClient.getJob(jobId).getDocument();
        String orignalDescription = job.getDescription();

        String desc1 = "a simple job";
        m_WebServiceClient.setJobDescription(jobId, desc1);
        job = m_WebServiceClient.getJob(jobId).getDocument();
        test(job != null);
        test(job.getDescription().equals(desc1));

        String emptyDesc = "";
        m_WebServiceClient.setJobDescription(jobId, emptyDesc);
        job = m_WebServiceClient.getJob(jobId).getDocument();
        test(job != null);
        test(job.getDescription().equals(emptyDesc));

        String longerDesc = "a little big longer\\nWith newline characters";
        m_WebServiceClient.setJobDescription(jobId, longerDesc);
        job = m_WebServiceClient.getJob(jobId).getDocument();
        test(job != null);
        test(job.getDescription().equals("a little big longer\nWith newline characters"));

        // Set the description back to what it was
        m_WebServiceClient.setJobDescription(jobId, orignalDescription);
        job = m_WebServiceClient.getJob(jobId).getDocument();
        test(job != null);
        test(job.getDescription().equals(orignalDescription));
    }


    /**
     * Tails the log files with requesting different numbers of lines
     * and checks that some content is present.
     * Downloads the zipped log files and checks for at least 2 files.
     *
     * @param jobId The job id
     *
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void testReadLogFiles(String jobId)
    throws ClientProtocolException, IOException
    {
        //tail
        String tail = m_WebServiceClient.tailLog(jobId, 2);
        String [] logLines = tail.split("\n");
        test(logLines.length > 0);
        test(logLines.length <= 2);

        tail = m_WebServiceClient.tailLog(jobId);
        logLines = tail.split("\n");
        test(logLines.length > 0);
        test(logLines.length <= 10);

        tail = m_WebServiceClient.tailLog(jobId, 50);
        logLines = tail.split("\n");
        test(logLines.length > 0);
        test(logLines.length <= 50);

        // whole file
        String file = m_WebServiceClient.downloadLog(jobId, "engine_api");
        logLines = file.split("\n");
        test(logLines.length > 0);

        file = m_WebServiceClient.downloadLog(jobId, jobId);
        logLines = file.split("\n");
        test(logLines.length > 0);

        // tail a named file
        tail = m_WebServiceClient.tailLog(jobId, "engine_api", 10);
        logLines = tail.split("\n");
        test(logLines.length > 0);
        test(logLines.length <= 10);

        tail = m_WebServiceClient.tailLog(jobId, jobId, 10);
        logLines = tail.split("\n");
        test(logLines.length > 0);
        test(logLines.length <= 10);

        // zip of log files
        try (ZipInputStream zip = m_WebServiceClient.downloadAllLogs(jobId))
        {
            ZipEntry entry = zip.getNextEntry();

            // expect at least 2 entries: the directory and 1 or more log files
            test(entry != null);
            test(entry.getName().equals(jobId + File.separator));
            entry = zip.getNextEntry();

            byte buff[] = new byte[2048];

            // log file(s)
            do
            {
                test(entry.getName().startsWith(jobId + File.separator));
                int len = zip.read(buff);
                test(len > 0);
                String content = new String(buff, StandardCharsets.UTF_8);
                logLines = content.split("\n");
                test(logLines.length > 0);
                entry = zip.getNextEntry();
            }
            while (entry != null);
        }

        /*
        // check errors by ask for a file that doesn't exist
        file = m_WebServiceClient.downloadLog(baseUrl, jobId, "not_a_file");
        test(file.isEmpty());
        ApiError error = m_WebServiceClient.getLastError();
        test(error != null);
        test(error.getErrorCode() == ErrorCode.MISSING_LOG_FILE);

        // get a file in a job that doesn't exist
        file = m_WebServiceClient.downloadLog(baseUrl, "not_a_job", "not_a_file");
        test(file.isEmpty());
        error = m_WebServiceClient.getLastError();
        test(error != null);
        test(error.getErrorCode() == ErrorCode.MISSING_LOG_FILE);
        */
    }


    /**
     * Delete all the jobs in the list of job ids
     *
     * @param jobIds The list of ids of the jobs to delete
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteJobsTest(List<String> jobIds)
    throws IOException, InterruptedException
    {
        for (String jobId : jobIds)
        {
            LOGGER.debug("Deleting job " + jobId);

            boolean success = m_WebServiceClient.deleteJob(jobId);
            if (success == false)
            {
                LOGGER.error("Error deleting job " + m_BaseUrl + "/" + jobId);
            }
        }

        for (String jobId : jobIds)
        {
            SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
            test(doc.isExists() == false);
        }

        for (String jobId : jobIds)
        {
            boolean success = m_WebServiceClient.deleteJob(jobId);
            test(success == false);
            ApiError error = m_WebServiceClient.getLastError();
            test(error.getErrorCode() == ErrorCodes.MISSING_JOB_ERROR);
        }
    }


    /**
     * Throws an exception if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }


    /**
     * The program takes one argument which is the base Url of the RESTful API.
     * If no arguments are given then {@value #API_BASE_URL} is used.
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args)
    throws IOException, InterruptedException
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        LOGGER.info("Testing Service at " + baseUrl);

        final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }


        JobsTest test = new JobsTest(baseUrl);
        List<String> jobUrls = new ArrayList<>();

        File flightCentreData = new File(prelertTestDataHome +
                "/engine_api_integration_test/flightcentre.csv.gz");
        File fareQuoteData = new File(prelertTestDataHome +
                "/engine_api_integration_test/farequote.csv");
        File flightCentreJsonData = new File(prelertTestDataHome +
                "/engine_api_integration_test/flightcentre.json");
        File flightCentreMsData = new File(prelertTestDataHome +
                "/engine_api_integration_test/flightcentre_ms.csv");
        File flightCentreMsJsonData = new File(prelertTestDataHome +
                "/engine_api_integration_test/flightcentre_ms.json");

        test.getJobsTest();

        // Always delete the test named jobs first in case they
        // are hanging around from a previous run
        test.m_WebServiceClient.deleteJob("flightcentre-csv");
        test.m_WebServiceClient.deleteJob("flightcentre-epoch-ms");


        //=================
        // CSV & Gzip test
        //
        String flightCentreJobId = test.createFlightCentreCsvJobTest();
        test.getJobsTest();

        test.testSetDescription(flightCentreJobId);
        test.uploadDataAndTestRecordsWereProcessed(flightCentreJobId, flightCentreData, true);
        test.closeJob(flightCentreJobId);
        test.testReadLogFiles(flightCentreJobId);
        test.verifyJobResults(flightCentreJobId, 100, FLIGHT_CENTRE_NUM_BUCKETS,
                3600, FLIGHT_CENTRE_NUM_EVENTS);
        test.testBucketScoreFilters(flightCentreJobId);
        jobUrls.add(flightCentreJobId);


        //=================
        // JSON test
        //
        String flightCentreJsonJobId = test.createFlightCentreJsonJobTest();
        test.getJobsTest();
        test.uploadDataAndTestRecordsWereProcessed(flightCentreJsonJobId, flightCentreJsonData, false);
        test.closeJob(flightCentreJsonJobId);
        test.testReadLogFiles(flightCentreJsonJobId);
        test.testSetDescription(flightCentreJsonJobId);
        test.verifyJobResults(flightCentreJsonJobId, 100, FLIGHT_CENTRE_NUM_BUCKETS,
                3600, FLIGHT_CENTRE_NUM_EVENTS);
        jobUrls.add(flightCentreJsonJobId);

        //=================
        // Time format test
        //
        String farequoteTimeFormatJobId = test.createFareQuoteTimeFormatJobTest();
        jobUrls.add(farequoteTimeFormatJobId);
        test.getJobsTest();

        test.slowUpload(farequoteTimeFormatJobId, fareQuoteData, 10);
        test.closeJob(farequoteTimeFormatJobId);
        test.verifyJobResults(farequoteTimeFormatJobId, 150, FARE_QUOTE_NUM_BUCKETS,
                300, FARE_QUOTE_NUM_EVENTS);
        test.testBucketScoreFilters(farequoteTimeFormatJobId);
        test.testReadLogFiles(farequoteTimeFormatJobId);
        test.testSetDescription(farequoteTimeFormatJobId);

        // known dates for the farequote data
        Date start = new Date(1359406800000L);
        Date end = new Date(1359662400000L);
        test.testBucketDateFilters(farequoteTimeFormatJobId, start, end);
        test.testRecordDateFilters(farequoteTimeFormatJobId, start, end);

        test.testSortingRecords(farequoteTimeFormatJobId, start, end);
        test.testRecordScoreFilters(farequoteTimeFormatJobId);

        //=====================================================
        // timestamp in ms from the epoch for both csv and json
        //
        String jobId = test.createFlightCentreMsCsvFormatJobTest(baseUrl, "flightcentre-epoch-ms");
        jobUrls.add(jobId);
        test.getJobsTest();
        test.uploadDataAndTestRecordsWereProcessed(jobId, flightCentreMsData, false);
        test.closeJob(jobId);
        test.verifyJobResults(jobId, 150, FLIGHT_CENTRE_NUM_BUCKETS,
                3600, FLIGHT_CENTRE_NUM_EVENTS);
        test.testReadLogFiles(jobId);
        test.testBucketDateFilters(jobId, new Date(1350824400000L), new Date(1350913371000L));

        jobId = test.createFlightCentreMsJsonFormatJobTest();
        jobUrls.add(jobId);
        test.getJobsTest();
        test.uploadDataAndTestRecordsWereProcessed(jobId, flightCentreMsJsonData, false);
        test.closeJob(jobId);
        test.verifyJobResults(jobId, 150, FLIGHT_CENTRE_NUM_BUCKETS,
                3600, FLIGHT_CENTRE_NUM_EVENTS);

        start = new Date(1350824400000L);
        end = new Date(1350913371000L);
        test.testBucketDateFilters(jobId, start, end);
        test.testRecordDateFilters(jobId, start, end);
        test.testSortingRecords(jobId, start, end);

        test.testReadLogFiles(jobId);


        //=================
        // double upload test (upload same file twice)
        //
        String doubleUploadTest = test.createFareQuoteTimeFormatJobTest();
        jobUrls.add(doubleUploadTest);

        test.uploadDataAndTestRecordsWereProcessed(doubleUploadTest, fareQuoteData, false);
        test.uploadDataAndTestNoRecordsWereProcessed(doubleUploadTest, fareQuoteData, false);

        test.closeJob(doubleUploadTest);
        test.verifyJobResults(doubleUploadTest, 150, FARE_QUOTE_NUM_BUCKETS,
                300, FARE_QUOTE_NUM_EVENTS);

        // known dates for the farequote data
        start = new Date(1359406800000L);
        end = new Date(1359662400000L);
        test.testBucketDateFilters(doubleUploadTest, start, end);

        //==========================
        // Clean up test jobs
        test.deleteJobsTest(jobUrls);
        test.close();

        LOGGER.info("All tests passed Ok");
    }

}
