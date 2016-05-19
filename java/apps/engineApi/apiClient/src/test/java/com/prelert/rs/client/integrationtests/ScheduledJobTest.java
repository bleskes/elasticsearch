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
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.ElasticsearchDataSourceCompatibility;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.rs.client.EngineApiClient;

public class ScheduledJobTest extends BaseIntegrationTest
{
    private static final String TEST_JOB_ID = "scheduled-job-test";

    private static final String START_SCHEDULER_URL_SUFFIX =
            "/schedulers/" + TEST_JOB_ID + "/start?end=%s";

    private static final String INDEX_NAME = "test-data-scheduled-job-test";
    private static final String TYPE_NAME = "record";
    private static final String ES_BASE_URL = "http://localhost:9200/";

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

    private String m_BaseUrl;
    private String m_EsBaseUrl;
    private String m_EsIndexUrl;
    private String m_EsRecordIndexUrl;
    private String m_EsRefreshIndexUrl;

    public ScheduledJobTest(String baseUrl, String esBaseUrl)
    {
        super(baseUrl);

        m_EsBaseUrl = esBaseUrl;
        m_EsIndexUrl = m_EsBaseUrl + INDEX_NAME + "/";
        m_EsRecordIndexUrl = m_EsIndexUrl + TYPE_NAME + "/";
        m_EsRefreshIndexUrl = m_EsIndexUrl + "_refresh";

        m_EngineApiClient = new EngineApiClient(m_BaseUrl);
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

    @Override
    public void runTest() throws IOException
    {
        deleteIndex();
        m_EngineApiClient.deleteJob(TEST_JOB_ID);

        generateDataInElasticsearch();
        createScheduledJob();
        startScheduler();
        waitUntilSchedulerStatusIs(JobSchedulerStatus.STOPPED);

        JobDetails job = m_EngineApiClient.getJob(TEST_JOB_ID).getDocument();
        test(job.getCounts().getInputRecordCount() == m_RecordsCount);
        test(job.getCounts().getProcessedRecordCount() == m_RecordsCount);

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

    private void generateDataInElasticsearch()
    {
        m_Logger.info("Generating data...");
        createDataIndex();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime currentTime = now.minus(30, ChronoUnit.DAYS);
        while (currentTime.isBefore(now))
        {
            String record = String.format(RECORD_TEMPLATE, currentTime.toEpochSecond());
            indexDocument(record);
            m_RecordsCount++;
            currentTime = currentTime.plus(1, ChronoUnit.MINUTES);
        }
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

    private void indexDocument(String jsonDoc)
    {
        try
        {
            m_HttpClient.POST(m_EsRecordIndexUrl)
                    .content(new StringContentProvider(jsonDoc))
                    .send();
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

    private String createScheduledJob() throws IOException
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

    private void startScheduler()
    {
        m_Logger.info("Starting scheduler");

        String startSchedulerUri = String.format(m_BaseUrl + START_SCHEDULER_URL_SUFFIX,
                ZonedDateTime.now().toEpochSecond());
        try
        {
            m_HttpClient.POST(startSchedulerUri).send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            m_Logger.error("Error while starting scheduler", e);
            throw new RuntimeException(e);
        }
    }

    private void waitUntilSchedulerStatusIs(JobSchedulerStatus status) throws IOException
    {
        m_Logger.info("Waiting for scheduler to stop");

        int count = 0;
        JobDetails job = m_EngineApiClient.getJob(TEST_JOB_ID).getDocument();
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

    public static void main(String[] args) throws IOException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        String esBaseUrl = ES_BASE_URL;
        if (args.length > 1)
        {
            esBaseUrl = args[1];
        }
        if (!esBaseUrl.endsWith("/"))
        {
            esBaseUrl += "/";
        }

        try (ScheduledJobTest test = new ScheduledJobTest(baseUrl, esBaseUrl))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }

    }
}
