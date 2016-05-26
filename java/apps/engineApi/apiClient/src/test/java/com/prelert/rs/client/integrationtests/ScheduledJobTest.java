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
import java.time.ZonedDateTime;

import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;

public class ScheduledJobTest extends BaseScheduledJobTest
{
    public ScheduledJobTest(String baseUrl, String esBaseUrl)
    {
        super(baseUrl, esBaseUrl);
    }

    @Override
    public void runTest() throws IOException
    {
        cleanUp();

        generateDataInElasticsearch(RECOMMENDED_BULK_UPLOAD_SIZE);
        createScheduledJob();
        startScheduler(ZonedDateTime.now().toEpochSecond());
        waitUntilSchedulerStatusIs(JobSchedulerStatus.STOPPED);

        JobDetails job = m_EngineApiClient.getJob(TEST_JOB_ID).getDocument();
        test(job.getCounts().getInputRecordCount() == getRecordsCount());
        test(job.getCounts().getProcessedRecordCount() == getRecordsCount());

        cleanUp();
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
