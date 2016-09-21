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

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.ElasticsearchDataSourceCompatibility;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.rs.client.EngineApiClient;

public abstract class BaseScheduledJobTest extends BaseIntegrationTest
{
    public static final int RECOMMENDED_BULK_UPLOAD_SIZE = 5000;

    protected static final String TEST_JOB_ID = "scheduled-job-test";

    private static final String BULK_INDEX_ACTION =
            "{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\"} }\n";

    protected static final String INDEX_NAME = "test-data-scheduled-job-test";
    protected static final String TYPE_NAME = "record";
    protected static final String ES_BASE_URL = "http://localhost:9200/";

    private static final String DATA_INDEX_MAPPINGS = "{"
            + "\"mappings\":{"
            + "  \"record\": {"
            + "    \"properties\": {"
            + "      \"timestamp\": {\"type\":\"date\"}"
            + "    }"
            + "  }"
            + "}"
            + "}";

    private static final String RECORD_TEMPLATE = "{"
            + "\"timestamp\": %s"
            + "}";

    private final HttpClient m_HttpClient;
    private int m_RecordsCount;

    private String m_EsBaseUrl;
    private String m_EsIndexUrl;
    private String m_EsRefreshIndexUrl;


    public BaseScheduledJobTest(String baseUrl, String esBaseUrl)
    {
        super(baseUrl);

        m_EsBaseUrl = esBaseUrl;
        m_EsIndexUrl = m_EsBaseUrl + INDEX_NAME + "/";
        m_EsRefreshIndexUrl = m_EsIndexUrl + "_refresh";

        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            m_Logger.error("Failed to start HTTP client", e);
            throw new RuntimeException(e);
        }
    }

    protected void cleanUp() throws IOException
    {
        deleteIndex();
        m_EngineApiClient.deleteJob(TEST_JOB_ID);
    }

    private void deleteIndex()
    {
        m_Logger.info("Deleting index: " + m_EsIndexUrl);
        try
        {
            ContentResponse response = m_HttpClient.newRequest(m_EsIndexUrl)
                    .method(HttpMethod.DELETE)
                    .send();
            m_Logger.info(response.getContentAsString());
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            m_Logger.error("Error uploading data to Elasticsearch", e);
        }
    }

    /**
     * Generate data and upload to Elastic
     * @param bulkUploadSize Number of documents in the bulk upload
     */
    protected void generateDataInElasticsearch(int bulkUploadSize)
    {
        String indexRequest = String.format(BULK_INDEX_ACTION, INDEX_NAME, TYPE_NAME);
        StringBuilder bulkRequest = new StringBuilder();

        // use bulk upload to make this quicker
        m_Logger.info("Generating data...");
        createDataIndex();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime currentTime = now.minus(30, ChronoUnit.DAYS);
        while (currentTime.isBefore(now))
        {
            bulkRequest.append(indexRequest);
            String record = String.format(RECORD_TEMPLATE, currentTime.toEpochSecond());
            bulkRequest.append(record).append("\n");

            m_RecordsCount++;

            if (m_RecordsCount % bulkUploadSize == 0)
            {
                bulkUpdate(bulkRequest.toString());
                bulkRequest.setLength(0);
            }
            currentTime = currentTime.plus(1, ChronoUnit.MINUTES);
        }

        bulkUpdate(bulkRequest.toString());

        refreshIndex();
        m_Logger.info("Completed data generation");
    }

    private void createDataIndex()
    {
        try
        {
            m_HttpClient.POST(m_EsIndexUrl)
                        .content(new StringContentProvider(DATA_INDEX_MAPPINGS))
                        .send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            m_Logger.error("Error uploading data to Elasticsearch", e);
            throw new RuntimeException(e);
        }
    }

    private void bulkUpdate(String jsonDocs)
    {
        try
        {
            ContentResponse response = m_HttpClient.POST(m_EsBaseUrl + "_bulk")
                    .content(new StringContentProvider(jsonDocs))
                    .send();

            m_Logger.info(response);
            if (response.getStatus() != 200)
            {
                throw new IllegalStateException("Non 200 status response from bulk upload : "
                            + response.toString());
            }
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            m_Logger.error("Error uploading data to Elasticsearch", e);
            throw new RuntimeException(e);
        }
    }

    private void refreshIndex()
    {
        m_Logger.info("Refreshing index...");
        try
        {
            m_HttpClient.POST(m_EsRefreshIndexUrl).send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            m_Logger.error("Error refreshing index", e);
            throw new RuntimeException(e);
        }
    }

    protected String createScheduledJob() throws IOException
    {
        Detector d = new Detector();
        d.setFunction("count");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(300L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.ELASTICSEARCH);
        dd.setTimeField("timestamp");
        dd.setTimeFormat("epoch");

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setDataSourceCompatibility(ElasticsearchDataSourceCompatibility.V_2_X_X.getDescription());
        schedulerConfig.setBaseUrl(m_EsBaseUrl);
        schedulerConfig.setIndexes(Arrays.asList(INDEX_NAME));
        schedulerConfig.setTypes(Arrays.asList(TYPE_NAME));

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Scheduled job test");
        config.setId(TEST_JOB_ID);
        config.setDataDescription(dd);
        config.setSchedulerConfig(schedulerConfig);

        String jobId = m_EngineApiClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            m_Logger.error("No Job Id returned by create job");
            m_Logger.error(m_EngineApiClient.getLastError().toJson());
            test(jobId != null && jobId.isEmpty() == false);
        }

        m_Logger.info("Created job: " + TEST_JOB_ID);

        return jobId;
    }

    protected boolean startScheduler(EngineApiClient client) throws IOException
    {
        m_Logger.info("Starting scheduler");

        return client.startScheduler(TEST_JOB_ID);
    }

    protected boolean startScheduler(EngineApiClient client, long endTimeEpochSeconds) throws IOException
    {
        m_Logger.info("Starting scheduler end=" + endTimeEpochSeconds);

        return client.startScheduler(TEST_JOB_ID, "", Long.toString(endTimeEpochSeconds));
    }

    protected boolean stopScheduler(EngineApiClient client) throws IOException
    {
        m_Logger.info("Stopping scheduler");

        return client.stopScheduler(TEST_JOB_ID);
    }

    protected void waitUntilSchedulerStatusIs(EngineApiClient client, JobSchedulerStatus status)
            throws IOException
    {
        m_Logger.info("Waiting for scheduler status to be " + status);

        int count = 0;
        JobDetails job = client.getJob(TEST_JOB_ID).getDocument();
        test(job != null, "Get job returned null, probably already deleted");
        while (job.getSchedulerStatus() != status)
        {
            if (count > 60)
            {
                m_Logger.error("Waiting for scheduler to finish timed out after " + count + " seconds");
                test(false);
            }
            count++;
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException e)
            {
                throw new IOException(e);
            }
            job = m_EngineApiClient.getJob(TEST_JOB_ID).getDocument();
        }
        m_Logger.info("Scheduler has stopped");
    }

    @Override
    public void close() throws IOException
    {
        super.close();

        try
        {
            m_HttpClient.stop();
        }
        catch (Exception e)
        {
            m_Logger.error("Failed to stop HTTP client", e);
        }
    }

    protected int getRecordsCount()
    {
        return m_RecordsCount;
    }

}
