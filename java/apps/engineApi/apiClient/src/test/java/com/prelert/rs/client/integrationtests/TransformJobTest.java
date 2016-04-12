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
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
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
 * Create a job with a concat transform and test the transform is applied.
 * Assuming the job has created anomaly records check that the by field value
 * looks like it has been concatenated by matching a regex.
 *
 * Also creates a 2nd job where the input to the date transform is the output of
 * concat this simply checks that the records are processed without any date
 * parse errors
 *
 * This isn't really a test of the concat transform but a test that transforms
 * work - concat is the simplest to test.
 */
public class TransformJobTest implements Closeable {
    private static final Logger LOGGER = Logger
            .getLogger(TransformJobTest.class);

    private static final String CONCAT_METRIC_JOB = "concat-metricname-test";
    private static final String CONCAT_DATE_JOB = "concat-date-test";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private EngineApiClient m_WebServiceClient;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public TransformJobTest(String baseUrl) {
        m_WebServiceClient = new EngineApiClient(baseUrl);
    }

    @Override
    public void close() throws IOException {
        m_WebServiceClient.close();
    }

    public void createDateConcatJob(boolean useJson) throws IOException
    {
        String JOB_CONFIG = "{\"id\":\"concat-date-test\","
                + "\"description\":\"Transform Job\","
                + "\"analysisConfig\" : {"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]},"
                + "\"transforms\":[{\"transform\":\"concat\", \"inputs\":[\"date\", \"time\"], \"outputs\":\"datetime\"}],";

        if (useJson)
        {
            JOB_CONFIG +=  "\"dataDescription\":{\"format\":\"JSON\", \"timeField\":\"datetime\", \"timeFormat\":\"yyyy-MM-ddHH:mm:ssX\"} }";
        }
        else
        {
            JOB_CONFIG += "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"datetime\", \"timeFormat\":\"yyyy-MM-ddHH:mm:ssX\"} }";
        }


        m_WebServiceClient.deleteJob(CONCAT_DATE_JOB);

        String jobId = m_WebServiceClient.createJob(JOB_CONFIG);
        if (jobId == null || jobId.isEmpty()) {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals(CONCAT_DATE_JOB));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false) {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();

        TransformConfig tr = new TransformConfig();
        tr.setTransform("concat");
        tr.setInputs(Arrays.asList("date", "time"));
        tr.setOutputs(Arrays.asList("datetime"));

        test(job.getTransforms().size() == 1);
        test(tr.equals(job.getTransforms().get(0)));
    }

    public void createSplitMetricJob(boolean useJson) throws IOException
    {
        String TRANSFORM_JOB_CONFIG = "{\"id\":\"concat-metricname-test\","
                + "\"description\":\"Transform Job\","
                + "\"analysisConfig\" : {"
                + "\"detectors\":[{\"fieldName\":\"value\",\"byFieldName\":\"instance_metric\"}]},"
                + "\"transforms\":[{\"transform\":\"concat\", \"inputs\":[\"instance\", \"metric\"], \"outputs\":\"instance_metric\"}],";

        if (useJson)
        {
            TRANSFORM_JOB_CONFIG +=  "\"dataDescription\":{\"format\":\"JSON\", \"timeField\":\"timestamp\", \"timeFormat\":\"epoch\"} }";
        }
        else
        {
            TRANSFORM_JOB_CONFIG += "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"timestamp\", \"timeFormat\":\"epoch\"} }";
        }



        m_WebServiceClient.deleteJob(CONCAT_METRIC_JOB);

        String jobId = m_WebServiceClient.createJob(TRANSFORM_JOB_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals(CONCAT_METRIC_JOB));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(jobId);
        if (doc.isExists() == false)
        {
            LOGGER.error("No Job at URL " + jobId);
        }
        JobDetails job = doc.getDocument();


        Detector d = new Detector();
        d.setDetectorDescription("metric(value) by instance_metric");
        d.setFunction("metric");
        d.setFieldName("value");
        d.setByFieldName("instance_metric");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        if (useJson)
        {
            dd.setFormat(DataFormat.JSON);
        }
        else
        {
            dd.setFormat(DataFormat.DELIMITED);
            dd.setFieldDelimiter(',');
        }
        dd.setTimeFormat("epoch");
        dd.setTimeField("timestamp");

        test(job.getDescription().equals("Transform Job"));

        test(ac.equals(job.getAnalysisConfig()));
        test(dd.equals(job.getDataDescription()));


        TransformConfig tr = new TransformConfig();
        tr.setTransform("concat");
        tr.setInputs(Arrays.asList("instance", "metric"));
        tr.setOutputs(Arrays.asList("instance_metric"));

        test(job.getTransforms().size() == 1);
        test(tr.equals(job.getTransforms().get(0)));
    }

    /**
     * Upload the contents of <code>dataFile</code> to the server.
     *
     * @param jobId
     *            The Job's Id
     * @param dataFile
     *            Should match the data configuration format of the job
     * @param compressed
     *            Is the data gzipped compressed?
     * @throws IOException
     */
    public void uploadData(String jobId, File dataFile, boolean compressed)
            throws IOException {
        FileInputStream stream = new FileInputStream(dataFile);
        MultiDataPostResult result = m_WebServiceClient.streamingUpload(jobId,
                stream, compressed);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);
        test(result.getResponses().get(0).getUploadSummary()
                .getProcessedRecordCount() > 0);
        test(result.getResponses().get(0).getUploadSummary()
                .getInvalidDateCount() == 0);
        test(result.getResponses().get(0).getUploadSummary()
                .getExcludedRecordCount() == 0);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.RUNNING);
    }

    /**
     * Finish the job (as all data has been uploaded).
     *
     * @param jobId
     *            The Job's Id
     * @return
     * @throws IOException
     */
    public boolean closeJob(String jobId) throws IOException {
        boolean closed = m_WebServiceClient.closeJob(jobId);
        test(closed);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.CLOSED);

        return closed;
    }

    /**
     * Assuming some anomaly records are generated make sure the by field value
     * looks like it has been concatenated by matching a regex
     *
     * @param baseUrl
     * @param jobId
     * @return
     * @throws IOException
     */
    public boolean checkRecordsHaveConcattedField(String baseUrl, String jobId)
            throws IOException {
        Pagination<AnomalyRecord> records = m_WebServiceClient
                .prepareGetRecords(jobId).take(50)
                .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
                .descending(true).get();

        test(records.getDocumentCount() > 0);

        // The concatenated fields are something like i-1f501643DiskReadBytes
        Pattern p = Pattern.compile("i-[a-f0-9]{8}[a-zA-Z]*");
        for (AnomalyRecord r : records.getDocuments()) {
            Matcher matcher = p.matcher(r.getByFieldValue());
            test(matcher.matches());
        }

        return true;
    }

    /**
     * Throws an exception if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition) throws IllegalStateException {
        if (condition == false) {
            throw new IllegalStateException();
        }
    }

    public static void main(String[] args) throws IOException
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        String baseUrl = API_BASE_URL;
        if (args.length > 0) {
            baseUrl = args[0];
        }

        LOGGER.info("Testing Service at " + baseUrl);

        final String prelertTestDataHome = System
                .getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }

        File splitMetricDataFileCsv = new File(
                prelertTestDataHome
                + "/engine_api_integration_test/transforms/aws_instance_metric_spit.csv");
        File splitDateTimeDataFileCsv = new File(prelertTestDataHome
                + "/engine_api_integration_test/transforms/split_date_time.csv");

        File splitMetricDataFileJson = new File(
                prelertTestDataHome
                + "/engine_api_integration_test/transforms/aws_instance_metric_spit.json");
        File splitDateTimeDataFileJson = new File(
                prelertTestDataHome
                + "/engine_api_integration_test/transforms/split_date_time.json");

        try (TransformJobTest transformTest = new TransformJobTest(baseUrl)) {
            // first with CSV data
            LOGGER.info("Running CSV tests");
            transformTest.createSplitMetricJob(false);
            transformTest.uploadData(CONCAT_METRIC_JOB, splitMetricDataFileCsv,
                    false);
            transformTest.closeJob(CONCAT_METRIC_JOB);
            transformTest.checkRecordsHaveConcattedField(baseUrl,
                    CONCAT_METRIC_JOB);

            transformTest.createDateConcatJob(false);
            transformTest.uploadData(CONCAT_DATE_JOB, splitDateTimeDataFileCsv,
                    false);
            transformTest.closeJob(CONCAT_DATE_JOB);

            // now with JSON
            LOGGER.info("Running JSON tests");
            transformTest.createSplitMetricJob(true);
            transformTest.uploadData(CONCAT_METRIC_JOB, splitMetricDataFileJson,
                    false);
            transformTest.closeJob(CONCAT_METRIC_JOB);
            transformTest.checkRecordsHaveConcattedField(baseUrl,
                    CONCAT_METRIC_JOB);

            transformTest.createDateConcatJob(true);
            transformTest.uploadData(CONCAT_DATE_JOB, splitDateTimeDataFileJson,
                    false);
            transformTest.closeJob(CONCAT_DATE_JOB);
        }

        LOGGER.info("All tests passed Ok");
    }

}
