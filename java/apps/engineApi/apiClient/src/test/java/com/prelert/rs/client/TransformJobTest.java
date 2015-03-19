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

package com.prelert.rs.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.TransformConfig;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Create a job with a concat transform and test the transform is applied.
 * Assuming the job has created anomaly records check that the by field value
 * looks like it has been concatenated by matching a regex.
 *
 * This isn't really a test of the concat transform but a test that
 * transforms work - concat is the simplest to test.
 */
public class TransformJobTest implements Closeable
{
	private static final Logger LOGGER = Logger.getLogger(TransformJobTest.class);

    private static final String CONCAT_METRIC_JOB = "concat-metricname-test";
    private static final String CONCAT_DATE_JOB = "concat-date-test";

	/**
	 * The default base Url used in the test
	 */
	public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

	private EngineApiClient m_WebServiceClient;

	/**
	 * Creates a new http client call {@linkplain #close()} once finished
	 */
	public TransformJobTest()
	{
		m_WebServiceClient = new EngineApiClient();
	}

	@Override
	public void close() throws IOException
	{
		m_WebServiceClient.close();
	}

	public void createDateConcatJob(String baseUrl) throws ClientProtocolException, IOException
    {
        final String JOB_CONFIG = "{\"id\":\"concat-date-test\","
                + "\"description\":\"Transform Job\","
                + "\"analysisConfig\" : {"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]},"
                + "\"transforms\":[{\"transform\":\"concat\", \"inputs\":[\"date\", \"time\"], \"outputs\":\"datetime\"}],"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"datetime\", "
                + "\"timeFormat\":\"yyyy-MM-ddHH:mm:ssX\"} }";


        m_WebServiceClient.deleteJob(baseUrl, CONCAT_DATE_JOB);

        String jobId = m_WebServiceClient.createJob(baseUrl, JOB_CONFIG);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error(m_WebServiceClient.getLastError().toJson());
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null);
        }
        test(jobId.equals(CONCAT_DATE_JOB));

        // get job by location, verify
        SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(baseUrl, jobId);
        if (doc.isExists() == false)
        {
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

	public void createSplitMetricJob(String baseUrl) throws ClientProtocolException, IOException
	{
		final String TRANSFORM_JOB_CONFIG = "{\"id\":\"concat-metricname-test\","
				+ "\"description\":\"Transform Job\","
				+ "\"analysisConfig\" : {"
				+ "\"detectors\":[{\"fieldName\":\"value\",\"byFieldName\":\"instance_metric\"}]},"
				+ "\"transforms\":[{\"transform\":\"concat\", \"inputs\":[\"instance\", \"metric\"], \"outputs\":\"instance_metric\"}],"
				+ "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"timestamp\", "
				+ "\"timeFormat\":\"epoch\"} }}";

		m_WebServiceClient.deleteJob(baseUrl, CONCAT_METRIC_JOB);

		String jobId = m_WebServiceClient.createJob(baseUrl, TRANSFORM_JOB_CONFIG);
		if (jobId == null || jobId.isEmpty())
		{
			LOGGER.error(m_WebServiceClient.getLastError().toJson());
			LOGGER.error("No Job Id returned by create job");
			test(jobId != null);
		}
		test(jobId.equals(CONCAT_METRIC_JOB));

		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(baseUrl, jobId);
		if (doc.isExists() == false)
		{
			LOGGER.error("No Job at URL " + jobId);
		}
		JobDetails job = doc.getDocument();


		Detector d = new Detector();
		d.setFieldName("value");
		d.setByFieldName("instance_metric");

		AnalysisConfig ac = new AnalysisConfig();
		ac.setDetectors(Arrays.asList(d));

		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(',');
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
     * @param baseUrl The URL of the REST API i.e. an URL like
     *  <code>http://prelert-host:8080/engine/version/</code>
     * @param jobId The Job's Id
     * @param dataFile Should match the data configuration format of the job
     * @param compressed Is the data gzipped compressed?
     * @throws IOException
     */
    public void uploadData(String baseUrl, String jobId, File dataFile, boolean compressed)
    throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);
        DataCounts counts = m_WebServiceClient.streamingUpload(baseUrl, jobId, stream, compressed);
        test(counts.getProcessedRecordCount() > 0);
        test(counts.getInvalidDateCount() == 0);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(baseUrl, jobId);
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
    public boolean closeJob(String baseUrl, String jobId)
    throws IOException
    {
        boolean closed = m_WebServiceClient.closeJob(baseUrl, jobId);
        test(closed);

        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(baseUrl, jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.CLOSED);

        return closed;
    }

    /**
     * Assuming some anomaly records are generated make sure the
     * by field value looks like it has been concatenated by
     * matching a regex
     *
     * @param baseUrl
     * @param jobId
     * @return
     * @throws IOException
     */
    public boolean checkRecordsHaveConcattedField(String baseUrl, String jobId) throws IOException
    {
        Pagination<AnomalyRecord> records = m_WebServiceClient.getRecords(baseUrl, jobId,
                0l, 50l, null, null, AnomalyRecord.NORMALIZED_PROBABILITY, true, null, null);

        test(records.getDocumentCount() > 0);

        // The concatenated fields are something like i-1f501643DiskReadBytes
        Pattern p = Pattern.compile("i-[a-f0-9]{8}[a-zA-Z]*");
        for (AnomalyRecord r : records.getDocuments())
        {
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
            LOGGER.error("Error property prelert.test.data.home is not set");
            return;
        }

        File splitMetricDataFile = new File(prelertTestDataHome +
                "/engine_api_integration_test/transforms/aws_instance_metric_spit.csv");
        File splitDateTimeDataFile = new File(prelertTestDataHome +
                "/engine_api_integration_test/transforms/split_date_time.csv");

		try (TransformJobTest transformTest = new TransformJobTest())
		{
			transformTest.createSplitMetricJob(baseUrl);
			transformTest.uploadData(baseUrl, CONCAT_METRIC_JOB, splitMetricDataFile, false);
			transformTest.closeJob(baseUrl, CONCAT_METRIC_JOB);
			transformTest.checkRecordsHaveConcattedField(baseUrl, CONCAT_METRIC_JOB);

            transformTest.createDateConcatJob(baseUrl);
            transformTest.uploadData(baseUrl, CONCAT_DATE_JOB, splitDateTimeDataFile, false);
            transformTest.closeJob(baseUrl, CONCAT_DATE_JOB);
		}

		LOGGER.info("All tests passed Ok");
	}

}
