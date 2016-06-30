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

import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.datauploader.CsvDataRunner;
import com.prelert.rs.client.integrationtests.BaseIntegrationTest;
import com.prelert.rs.data.EngineStatus;

public class DistributedStatusTest extends BaseIntegrationTest
{
    private String [] m_EngineApiUrls;

    public DistributedStatusTest(String [] engineApiUrls)
    {
        super(engineApiUrls[0]);

        m_EngineApiUrls = engineApiUrls;
    }

    @Override
    protected void runTest() throws IOException
    {
        // start job on the first node, extract host name from url
        Pattern p = Pattern.compile("http://([a-zA-Z0-9\\.-]*):.*");
        Matcher m = p.matcher(m_EngineApiUrls[0]);
        test(m.matches());


        // create job on the first node and start uploading data
        CsvDataRunner dataUploader = startDataUploader(m_EngineApiUrls[0]);
        try
        {
            testEngineStatus(m_EngineApiUrls, dataUploader.getJobId(), m.group(1));
        }
        finally
        {
            stopDataUploaderAndJoinThread(dataUploader);
            m_EngineApiClient.closeJob(dataUploader.getJobId());
        }

        m_EngineApiClient.deleteJob(dataUploader.getJobId());
    }

    private void testEngineStatus(String [] engineUrls, String jobId, String jobHost)
    throws IOException
    {
        for (String url : engineUrls)
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
        // expect at least 2 hosts
        if (args.length <= 1)
        {
            // Logging not configured yet
            String msg = "At least 2 host URLS should be provided as arguments to main";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }


        try (DistributedStatusTest test = new DistributedStatusTest(args))
        {
            test.getLogger().info("Starting Distributed status test");
            test.runTest();
            test.getLogger().info("Distributed status test passed");
        }
    }
}
