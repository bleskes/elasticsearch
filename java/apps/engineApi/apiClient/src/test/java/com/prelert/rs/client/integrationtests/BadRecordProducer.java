/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;

/**
 *  Class to create bad records for testing error conditions in the API
 */
public class BadRecordProducer implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(BadRecordProducer.class);

    public static final String HEADER = "time,metric,value";

    public enum TestType {OUT_OF_ORDER_RECORDS, BAD_TIMESTAMP};

    private PipedOutputStream m_OutputStream;
    private TestType m_TestType;
    private long m_NumIterations;
    private boolean m_IsCancelled;


    /**
     * Create the bad record producer for the test type.
     *
     * @param testType The type of test to run
     * @param sink Data is written to this stream by connecting it
     * to a piped input stream.
     * @throws IOException
     */
    public BadRecordProducer(TestType testType, PipedInputStream sink)
    throws IOException
    {
        m_TestType = testType;
        m_NumIterations = 1000;
        m_OutputStream = new PipedOutputStream(sink);
        m_IsCancelled = false;
    }


    /**
     * Create a new job configuration each call rather than
     * a member that could be mutated.
     * @return
     */
    public JobConfiguration getJobConfiguration()
    {
        Detector d = new Detector();
        d.setFieldName("metric");
        d.setByFieldName("value");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat(DataDescription.EPOCH);

        JobConfiguration jc = new JobConfiguration(ac);
        jc.setDataDescription(dd);

        return jc;
    }

    public void setNumIterations(long numIter)
    {
        m_NumIterations = numIter;
    }

    @Override
    public void run()
    {
        // HACK wait for the parent thread to open the connection
        // before writing
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e1)
        {
            LOGGER.error("Producer interruputed pausing before write start");
        }

        try
        {
            int iterationCount = 0;
            long epoch = new Date().getTime() / 1000;
            writeHeader();

            if (m_TestType == TestType.BAD_TIMESTAMP)
            {
                while (++iterationCount <= m_NumIterations && !m_IsCancelled)
                {
                    writeTimeSeriesRow(1, epoch);
                    writeTimeSeriesBadTimestamp(1);

                    epoch++;
                }
            }
            else if (m_TestType == TestType.OUT_OF_ORDER_RECORDS)
            {
                // create a hundred records that are ok
                while (++iterationCount <= 100 && !m_IsCancelled)
                {
                    writeTimeSeriesRow(1, epoch);
                    epoch++;
                }

                // write older records
                epoch -= 60;
                while (++iterationCount <= 300 && !m_IsCancelled)
                {
                    writeTimeSeriesRow(1, epoch);
                }

            }

            System.out.println("final epoch = " + epoch);

        }
        finally
        {
            try
            {
                m_OutputStream.close();
            }
            catch (IOException e) {
                LOGGER.error("Error closing pipedoutputstream", e);
            }
        }
    }


    private void writeHeader()
    {
        try
        {
            m_OutputStream.write(HEADER.getBytes(StandardCharsets.UTF_8));
            m_OutputStream.write(10); // newline char
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing csv header", e);
        }
    }


    /**
     * Generate a random value for the time series using ThreadLocalRandom
     * @param timeSeriesId
     * @param epoch
     */
    private void writeTimeSeriesRow(long timeSeriesId, long epoch)
    {
        String timeSeries = "metric" + timeSeriesId;
        int value = ThreadLocalRandom.current().nextInt(512);

        String row = String.format("%d,%s,%d", epoch, timeSeries, value);
        try
        {
            m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
            m_OutputStream.write(10); // newline char
        }
        catch (IOException e)
        {
            LOGGER.warn("Error writing csv row", e);
            m_IsCancelled = true;
        }
    }

    /**
     * Write a time series record with an unreadable timestamp
     *
     * @param timeSeriesId
     */
    private void writeTimeSeriesBadTimestamp(long timeSeriesId)
    {
        String timeSeries = "metric" + timeSeriesId;
        int value = ThreadLocalRandom.current().nextInt(512);

        String row = String.format("%s,%s,%d", "", timeSeries, value);
        try
        {
            m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
            m_OutputStream.write(10); // newline char
        }
        catch (IOException e)
        {
            LOGGER.warn("Error writing csv row", e);
            m_IsCancelled = true;
        }
    }
}