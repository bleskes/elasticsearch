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

package com.prelert.rs.client.integrationtests.distributed;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.integrationtests.BaseScheduledJobTest;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.EngineStatus;

/**
 * Tests that a scheduled job cannot be stopped from another node
 * in a distributed system.
 * <br>
 * Populates Elasticsearch with data, starts the scheduled job then
 * checks various actions cannot be performed.
 */
public class DistributedScheduledJobTest extends BaseScheduledJobTest
{
    private String [] m_EngineApiUrls;

    public DistributedScheduledJobTest(String esBaseUrl, String [] engineApiUrls)
    {
        super(engineApiUrls[0], esBaseUrl);
        m_EngineApiUrls = engineApiUrls;
    }


    @Override
    protected void runTest() throws IOException
    {
        String [] otherHostUrls = new String [m_EngineApiUrls.length -1];
        System.arraycopy(m_EngineApiUrls, 1, otherHostUrls, 0, m_EngineApiUrls.length -1);

        cleanUp();

        generateDataInElasticsearch(RECOMMENDED_BULK_UPLOAD_SIZE);

        try
        {
            createScheduledJob();
            startScheduler();
            testCannotStartSchedulerOnOtherHosts(otherHostUrls);

            // start job on the first node, extract host name from url
            Pattern p = Pattern.compile("http://([a-zA-Z0-9\\.-]*):.*");
            Matcher m = p.matcher(m_BaseUrl);
            test(m.matches());
            testJobHostIsInStatus(TEST_JOB_ID, m.group(1));
        }
        finally
        {
            stopScheduler();
            waitUntilSchedulerStatusIs(JobSchedulerStatus.STOPPED);
            cleanUp();
        }
    }

    private void testCannotStartSchedulerOnOtherHosts(String [] otherHostUrls)
    throws IOException
    {
        String schedulerConfigUpdate = "{\"schedulerConfig\" : {"
      + "\"query\" : {\"match_all\" : { }},"
      + "\"types\" : [ \"record\" ],"
      + "\"queryDelay\" : 60,"
      + "\"dataSource\" : \"ELASTICSEARCH\","
      + "\"dataSourceCompatibility\" : \"2.x.x\","
      + "\"retrieveWholeSource\" : false,"
      + "\"baseUrl\" : \"http://localhost:9200/\","
      + "\"indexes\" : [ \"test-data-scheduled-job-test\" ],"
      + "\"scrollSize\" : 500}}";

        for (String host : otherHostUrls)
        {
            try (EngineApiClient client = new EngineApiClient(host))
            {
                boolean started = client.startScheduler(TEST_JOB_ID);
                test(started == false);
                ApiError error = client.getLastError();
                test(error.getErrorCode() == ErrorCodes.CANNOT_START_JOB_SCHEDULER);

                boolean stopped = client.stopScheduler(TEST_JOB_ID);
                test(stopped == false);
                error = client.getLastError();
                test(error.getErrorCode() == ErrorCodes.CANNOT_STOP_JOB_SCHEDULER);

                boolean deleted = client.deleteJob(TEST_JOB_ID);
                test(deleted == false);
                error = client.getLastError();
                test(error.getErrorCode() == ErrorCodes.CANNOT_STOP_JOB_SCHEDULER);

                // update sched conf
                boolean updated = client.updateJob(TEST_JOB_ID, schedulerConfigUpdate);
                test(updated == false);
                error = client.getLastError();
                test(error.getErrorCode() == ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
            }
        }
    }

    private void testJobHostIsInStatus(String jobId, String jobHost) throws IOException
    {
        for (String url : m_EngineApiUrls)
        {
            try (EngineApiClient client = new EngineApiClient(url))
            {
                EngineStatus status = client.status();
                test(status.getEngineHosts().size() > 1);
                test(jobHost, status.getHostByJob().get(jobId));
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length < 3)
        {
            String message = "DistributedScheduledJobTest requires at least 3 arguments.\n"
                        + "The first is the Elasticsearch URL followed by a list of Engine API URLs\n."
                        + "e.g. http://localhost:9200/ http://marple:8080/engine/v2 http://aws:8080/engine/v2";

            System.out.println(message);
            throw new IllegalArgumentException(message);
        }

        String esBaseUrl = args[0];
        if (!esBaseUrl.endsWith("/"))
        {
            esBaseUrl += "/";
        }

        String [] engineHostUrls = new String [args.length -1];
        System.arraycopy(args, 1, engineHostUrls, 0, args.length -1);

        try (DistributedScheduledJobTest test = new DistributedScheduledJobTest(esBaseUrl, engineHostUrls))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }
    }
}