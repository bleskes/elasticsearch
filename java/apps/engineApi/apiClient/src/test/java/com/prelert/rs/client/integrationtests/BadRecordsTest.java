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
import java.io.PipedInputStream;

import com.prelert.job.JobConfiguration;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;


/**
 * Upload data that has a high proportion of records that have
 * unparseable dates or are not in ascending time order.
 * Check the appropriate error code is returned.
 */
public class BadRecordsTest extends BaseIntegrationTest
{
    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public BadRecordsTest(String baseUrl)
    {
        super(baseUrl);
    }


    @Override
    public void runTest() throws IOException
    {
        testUnparseableDates();
        testOutOfOrderDates();
    }

    /**
     * Generate records with unparsable dates the streaming client
     * should return {@link ErrorCodes#TOO_MANY_BAD_DATES} error.
     *
     * @throws IOException
     */
    public void testUnparseableDates() throws IOException
    {
        PipedInputStream inputStream = new PipedInputStream();

        BadRecordProducer producer = new BadRecordProducer(
                BadRecordProducer.TestType.BAD_TIMESTAMP, inputStream);
        Thread producerThread = new Thread(producer, "Producer-Thread");

        JobConfiguration jc = producer.getJobConfiguration();
        jc.setDescription("Bad dates test");
        String jobId = m_EngineApiClient.createJob(jc);

        producerThread.start();

        MultiDataPostResult result = m_EngineApiClient.streamingUpload(jobId, inputStream, false);

        // Sometimes, due to the way the server cuts the connection when it
        // receives too much bad data, the exact error from the server can get
        // lost.  In this case there may be no response, so the anErrorOccurred()
        // call below can return false.  However, in this case we should still
        // have set an unknown last error on the client, so the user at least
        // has some way to find out something.  Therefore, we can tolerate a
        // small percentage of failures in the anErrorOccurred() assertion below,
        // but should never tolerate a failure in this assertion.
        test(m_EngineApiClient.getLastError() != null);

        test(result.anErrorOccurred());
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error != null);
        test(error.getErrorCode() == ErrorCodes.TOO_MANY_BAD_DATES);

        test(result.getResponses().get(0).getUploadSummary() == null);


        m_EngineApiClient.closeJob(jobId);

        try
        {
            producerThread.join();
        }
        catch (InterruptedException e)
        {
            m_Logger.error(e);
        }
    }


    /**
     * Generate records with that are not in ascending time order.
     *
     * The client should return with
     * {@link ErrorCodes#TOO_MANY_OUT_OF_ORDER_RECORDS} error.
     *
     * @throws IOException
     */
    public void testOutOfOrderDates() throws IOException
    {
        PipedInputStream inputStream = new PipedInputStream();

        BadRecordProducer producer = new BadRecordProducer(
                BadRecordProducer.TestType.OUT_OF_ORDER_RECORDS, inputStream);
        Thread producerThread = new Thread(producer, "Producer-Thread");

        JobConfiguration jc = producer.getJobConfiguration();
        jc.setDescription("Out of order records test");
        String jobId = m_EngineApiClient.createJob(jc);

        producerThread.start();

        MultiDataPostResult result = m_EngineApiClient.streamingUpload(jobId, inputStream, false);

        // Sometimes, due to the way the server cuts the connection when it
        // receives too much bad data, the exact error from the server can get
        // lost.  In this case there may be no response, so the anErrorOccurred()
        // call below can return false.  However, in this case we should still
        // have set an unknown last error on the client, so the user at least
        // has some way to find out something.  Therefore, we can tolerate a
        // small percentage of failures in the anErrorOccurred() assertion below,
        // but should never tolerate a failure in this assertion.
        test(m_EngineApiClient.getLastError() != null);

        test(result.anErrorOccurred());
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error != null);
        test(error.getErrorCode() == ErrorCodes.TOO_MANY_OUT_OF_ORDER_RECORDS);

        test(result.getResponses().get(0).getUploadSummary() == null);

        m_EngineApiClient.closeJob(jobId);

        try
        {
            producerThread.join();
        }
        catch (InterruptedException e)
        {
            m_Logger.error(e);
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
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }


        try (BadRecordsTest test = new BadRecordsTest(baseUrl))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }

    }
}
