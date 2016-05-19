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
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.SingleDocument;

/**
 * Base Integration test class containing the common funtions.
 * Sets up the logger and the web service client
 */
public abstract class BaseIntegrationTest implements AutoCloseable
{
    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    protected String m_TestDataHome;
    protected Logger m_Logger;
    protected String m_BaseUrl;
    protected EngineApiClient m_EngineApiClient;

    protected abstract void runTest() throws IOException;

    public BaseIntegrationTest(String baseUrl)
    {
        configureLogging();
        createLogger();

        m_BaseUrl = baseUrl;
        m_Logger.info("Testing the service at " + baseUrl);
        m_EngineApiClient = new EngineApiClient(baseUrl);
    }

    public BaseIntegrationTest(String baseUrl, boolean needsTestDataHome)
    {
        this(baseUrl);
        if (needsTestDataHome)
        {
            readTestDataHomeProperty();
        }
    }

    protected void createLogger()
    {
        m_Logger = Logger.getLogger(this.getClass());
    }

    protected void configureLogging()
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    protected void readTestDataHomeProperty()
    {
        final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }

        m_TestDataHome = prelertTestDataHome;
    }

    /**
     * Throws if <code>condition</code> is false
     *
     * @param condition
     * @throws IllegalStateException
     */
    public void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }

    @Override
    public void close() throws IOException
    {
        m_EngineApiClient.close();
    }

    /**
     * Throws if <code>condition</code> is false
     *
     * @param condition
     * @param message
     * @throws IllegalStateException
     */
    public void test(boolean condition, String message)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException(message);
        }
    }


    /**
     * Close the job returning the success of the operation
     *
     * @param jobId The Job's Id
     * @return
     */
    public boolean closeJob(String jobId) throws IOException
    {
        return m_EngineApiClient.closeJob(jobId);
    }

    /**
     * Finish the job (as all data has been uploaded).
     *
     * Throws if the job can't be closed
     *
     * @param jobId
     *            The Job's Id
     * @return
     * @throws IOException
     */
    public boolean closeJobThrow(String jobId) throws IOException
    {
        boolean closed = m_EngineApiClient.closeJob(jobId);
        test(closed);

        SingleDocument<JobDetails> job = m_EngineApiClient.getJob(jobId);
        test(job.isExists());
        test(job.getDocument().getStatus() == JobStatus.CLOSED);

        return closed;
    }

    /**
     * Delete the job.
     * Return false if the job wasn't deleted
     *
     * @param jobId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean deleteJob(String jobId)
    throws IOException
    {
        m_Logger.debug("Deleting job " + jobId);

        boolean success = m_EngineApiClient.deleteJob(jobId);
        if (success == false)
        {
            m_Logger.error("Error deleting job " + m_BaseUrl + "/" + jobId);
        }

        return success;
    }

    /**
     * Delete all the jobs in the list of job ids
     *
     * @param jobIds The list of ids of the jobs to delete
     * @throws IOException
     */
    public void deleteJobs(List<String> jobIds)
    throws IOException
    {
        for (String jobId : jobIds)
        {
            m_Logger.debug("Deleting job " + jobId);

            boolean success = m_EngineApiClient.deleteJob(jobId);
            if (success == false)
            {
                m_Logger.error("Error deleting job " + m_BaseUrl + "/" + jobId);
            }
        }
    }
}
