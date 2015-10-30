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

package com.prelert.rs.client.integrationtests;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.transform.TransformConfig;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Test the Highest registered domain transform with 2 jobs:
 * <ol><li>max(bytes) over highest registered domain</li>
 *     <li>info_content(sub-domain) over highest registered domain</li>
 * </ol>
 * The test asserts that there are anomalies and the over field values
 * are the actual highest registered domain
 */
public class HrdTransformTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(HrdTransformTest.class);

    private static final String MAX_BYTES_OVER_HRD_JOB = "max-bytes-over-hrd";
    private static final String INFO_CONTENT_OVER_HRD_JOB = "info-content-over-hrd";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

    private final EngineApiClient m_WebServiceClient;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public HrdTransformTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public void createMaxBytesOverHrdJob() throws ClientProtocolException, IOException
    {
        final String JOB_CONFIG = "{\"id\":\"" + MAX_BYTES_OVER_HRD_JOB + "\","
                + "\"analysisConfig\" : {"
                    + "\"bucketSpan\":600,"
                    + "\"detectors\":[{\"fieldName\":\"sum_cs_bytes_\",\"function\":\"max\", \"overFieldName\":\"highest_registered_domain\"}]},"
                    + "\"transforms\":[{\"transform\":\"domain_split\", \"inputs\":\"cs_host\", \"outputs\":[\"sub_domain\", \"highest_registered_domain\"]}],"
                    + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", "
                        + "\"timeFormat\":\"yyyy-MM-dd HH:mm:ss.SSS z\"} }";


        m_WebServiceClient.deleteJob(MAX_BYTES_OVER_HRD_JOB);

        String jobId = m_WebServiceClient.createJob(JOB_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals(MAX_BYTES_OVER_HRD_JOB));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        TransformConfig tr = new TransformConfig();
        tr.setTransform("domain_split");
        tr.setInputs(Arrays.asList("cs_host"));
        tr.setOutputs(Arrays.asList("sub_domain", "highest_registered_domain"));

        test(job.getTransforms().size() == 1);
        test(tr.equals(job.getTransforms().get(0)));

    }

    public void createInfoContentOverHrdJob() throws ClientProtocolException, IOException
    {
        final String TRANSFORM_JOB_CONFIG = "{\"id\":\"" + INFO_CONTENT_OVER_HRD_JOB + "\","
                + "\"analysisConfig\" : {"
                + "\"bucketSpan\":600,"
                + "\"detectors\":[{\"fieldName\":\"sub_domain\",\"function\":\"info_content\", \"overFieldName\":\"highest_registered_domain\"}]},"
                + "\"transforms\":[{\"transform\":\"domain_split\", \"inputs\":\"cs_host\", \"outputs\":[\"sub_domain\", \"highest_registered_domain\"]}],"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", "
                    + "\"timeFormat\":\"yyyy-MM-dd HH:mm:ss.SSS z\"} }";


        m_WebServiceClient.deleteJob(INFO_CONTENT_OVER_HRD_JOB);

        String jobId = m_WebServiceClient.createJob(TRANSFORM_JOB_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals(INFO_CONTENT_OVER_HRD_JOB));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        TransformConfig tr = new TransformConfig();
        tr.setTransform("domain_split");
        tr.setInputs(Arrays.asList("cs_host"));
        tr.setOutputs(Arrays.asList("sub_domain", "highest_registered_domain"));

        test(job.getTransforms().size() == 1);
        test(tr.equals(job.getTransforms().get(0)));
    }

    /**
     * Upload the contents of <code>dataFile</code> to the server.
     *
     * @param baseUrl The URL of the REST API i.e. an URL like
     *  <code>http://prelert-host:8080/engine/version/</code>
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param compressed Is the data gzipped compressed?
     * @throws IOException
     */
    public void uploadData(String jobId, File dataFile, boolean compressed)
    throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);
        MultiDataPostResult result = m_WebServiceClient.streamingUpload(jobId, stream, compressed);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);
        test(result.getResponses().get(0).getUploadSummary().getProcessedRecordCount() > 0);
        test(result.getResponses().get(0).getUploadSummary().getInvalidDateCount() == 0);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.RUNNING);
    }

    /**
     * Finish the job (as all data has been uploaded).
     *
     * @param baseUrl The URL of the REST API i.e. an URL like
     *  <code>http://prelert-host:8080/engine/version/</code>
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
     * Assuming some anomaly records are generated make sure the
     * over field value looks like an top level domain i.e. it
     * ends in .com, .net, etc
     *
     * @param jobId
     * @return
     * @throws IOException
     */
    public boolean checkOverFieldValuesAreTopLevelDomains(String jobId) throws IOException
    {
        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(jobId)
                        .take(50)
                        .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
                        .descending(true).get();

        test(records.getDocumentCount() > 0);

        for (AnomalyRecord r : records.getDocuments())
        {
            test("highest_registered_domain".equals(r.getOverFieldName()));
            boolean looksLikeATopLevelDomain = r.getOverFieldValue().endsWith(".com") ||
                                    r.getOverFieldValue().endsWith(".net") ||
                                    r.getOverFieldValue().endsWith(".co.uk") ||
                                    r.getOverFieldValue().endsWith(".io") ||
                                    r.getOverFieldValue().endsWith(".net.au") ||
                                    r.getOverFieldValue().endsWith(".edu.au");

            if (!looksLikeATopLevelDomain)
            {
                LOGGER.error(r.getOverFieldValue() + " does not look like a top level domain");
            }

            test(looksLikeATopLevelDomain);
        }

        return true;
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

    public static void main(String[] args) throws ClientProtocolException, IOException
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

        File splitMetricDataFile = new File(prelertTestDataHome +
                "/engine_api_integration_test/transforms/hrd/hrd_test.csv");
        File splitDateTimeDataFile = new File(prelertTestDataHome +
                "/engine_api_integration_test/transforms/hrd/hrd_test.csv");

        try (HrdTransformTest transformTest = new HrdTransformTest(baseUrl))
        {
            transformTest.createMaxBytesOverHrdJob();
            transformTest.uploadData(MAX_BYTES_OVER_HRD_JOB, splitMetricDataFile, false);
            transformTest.closeJob(MAX_BYTES_OVER_HRD_JOB);

            transformTest.checkOverFieldValuesAreTopLevelDomains(MAX_BYTES_OVER_HRD_JOB);

            transformTest.createInfoContentOverHrdJob();
            transformTest.uploadData(INFO_CONTENT_OVER_HRD_JOB, splitDateTimeDataFile, false);
            transformTest.closeJob(INFO_CONTENT_OVER_HRD_JOB);

            transformTest.checkOverFieldValuesAreTopLevelDomains(INFO_CONTENT_OVER_HRD_JOB);
        }

        LOGGER.info("All tests passed Ok");
    }

}
