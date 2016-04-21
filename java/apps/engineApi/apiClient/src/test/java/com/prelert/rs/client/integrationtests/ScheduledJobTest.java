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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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

public class ScheduledJobTest implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(ScheduledJobTest.class);

    private static final String TEST_JOB_ID = "scheduled-job-test";

    /**
     * The default base Url used in the test
     */
    private static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private static final String START_SCHEDULER_URL_TEMPLATE =
            API_BASE_URL + "/schedulers/" + TEST_JOB_ID + "/start?end=%s";

    private static final String INDEX_NAME = "test-data-scheduled-job-test";
    private static final String TYPE_NAME = "record";
    private static final String ES_BASE_URL = "http://localhost:9200/";
    private static final String ES_INDEX_URL = ES_BASE_URL + INDEX_NAME + "/";
    private static final String ES_RECORD_INDEX_URL = ES_INDEX_URL + TYPE_NAME + "/";
    private static final String ES_REFRESH_INDEX_URL = ES_INDEX_URL + "_refresh";

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

    private final EngineApiClient m_EngineApiClient;
    private final HttpClient m_HttpClient;
    private int m_RecordsCount;

    public ScheduledJobTest()
    {
        m_EngineApiClient = new EngineApiClient(API_BASE_URL);
        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to start HTTP client", e);
            throw new RuntimeException(e);
        }
    }

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
        LOGGER.info("Deleting index: " + ES_INDEX_URL);
        try
        {
            ContentResponse response = m_HttpClient.newRequest(ES_INDEX_URL)
                    .method(HttpMethod.DELETE)
                    .send();
            LOGGER.info(response.getContentAsString());
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("Error uploading data to Elasticsearch", e);
        }
    }

    private void generateDataInElasticsearch()
    {
        LOGGER.info("Generating data...");
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
        LOGGER.info("Completed data generation");
    }

    private void createDataIndex()
    {
        try
        {
            m_HttpClient.POST(ES_INDEX_URL)
            .content(new StringContentProvider(DATA_INDEX_MAPPINGS))
            .send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("Error uploading data to Elasticsearch", e);
            throw new RuntimeException(e);
        }
    }

    private void indexDocument(String jsonDoc)
    {
        try
        {
            m_HttpClient.POST(ES_RECORD_INDEX_URL)
                    .content(new StringContentProvider(jsonDoc))
                    .send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("Error uploading data to Elasticsearch", e);
            throw new RuntimeException(e);
        }
    }

    private void refreshIndex()
    {
        LOGGER.info("Refreshing index...");
        try
        {
            m_HttpClient.POST(ES_REFRESH_INDEX_URL).send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("Error refreshing index", e);
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
        schedulerConfig.setDataSourceCompatibility(ElasticsearchDataSourceCompatibility.V_2_X_X.toString());
        schedulerConfig.setBaseUrl(ES_BASE_URL);
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
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }

        LOGGER.info("Created job: " + TEST_JOB_ID);

        return jobId;
    }

    private void startScheduler()
    {
        LOGGER.info("Starting scheduler");

        String startSchedulerUri = String.format(START_SCHEDULER_URL_TEMPLATE,
                ZonedDateTime.now().toEpochSecond());
        try
        {
            m_HttpClient.POST(startSchedulerUri).send();
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("Error while starting scheduler", e);
            throw new RuntimeException(e);
        }
    }

    private void waitUntilSchedulerStatusIs(JobSchedulerStatus status) throws IOException
    {
        LOGGER.info("Waiting for scheduler to stop");

        int count = 0;
        JobDetails job = m_EngineApiClient.getJob(TEST_JOB_ID).getDocument();
        while (job.getSchedulerStatus() != status)
        {
            if (count > 60)
            {
                LOGGER.error("Waiting for scheduler to finish timed out after " + count + " seconds");
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
        LOGGER.info("Scheduler has stopped");
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            m_HttpClient.stop();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to stop HTTP client", e);
        }
        m_EngineApiClient.close();
    }

    /**
     * Throws an IllegalStateException if <code>condition</code> is false.
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

    public static void main(String[] args) throws IOException
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        try (ScheduledJobTest test = new ScheduledJobTest())
        {
            test.runTest();
        }

        LOGGER.info("All tests passed Ok");
    }
}
