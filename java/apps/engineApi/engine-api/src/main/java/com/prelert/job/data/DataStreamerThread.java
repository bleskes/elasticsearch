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

package com.prelert.job.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.prelert.job.DataCounts;
import com.prelert.job.JobException;
import com.prelert.job.process.params.DataLoadParams;

public class DataStreamerThread extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(DataStreamerThread.class);

    private DataCounts m_Stats;
    private final String m_JobId;
    private final String  m_ContentEncoding;
    private final DataLoadParams m_Params;
    private final InputStream m_Input;
    private final DataStreamer m_DataStreamer;
    private JobException m_JobException;
    private IOException m_IOException;

    public DataStreamerThread(DataStreamer dataStreamer, String jobId, String contentEncoding,
            DataLoadParams params, InputStream input)
    {
        super("DataStreamer-" + jobId);

        m_DataStreamer = dataStreamer;
        m_JobId = jobId;
        m_ContentEncoding = contentEncoding;
        m_Params = params;
        m_Input = input;
    }

    @Override
    public void run()
    {
        try
        {
            m_Stats = m_DataStreamer.streamData(m_ContentEncoding, m_JobId, m_Input, m_Params);
        }
        catch (JobException e)
        {
            m_JobException = e;
        }
        catch (IOException e)
        {
            m_IOException = e;
        }
        finally
        {
            try
            {
                m_Input.close();
            }
            catch (IOException e)
            {
                LOGGER.warn("Exception closing the data input stream", e);
            }
        }
    }

    /**
     * This method should only be called <b>after</b> the thread
     * has joined other wise the result could be <code>null</code>
     * (best case) or undefined.
     * @return
     */
    public DataCounts getDataCounts()
    {
        return m_Stats;
    }

    /**
     * If a Job exception was thrown during the run of this thread it
     * is accessed here. Only call this method after the thread has joined.
     * @return
     */
    public Optional<JobException> getJobException()
    {
        return Optional.ofNullable(m_JobException);
    }

    /**
     * If an IOException was thrown during the run of this thread it
     * is accessed here. Only call this method after the thread has joined.
     * @return
     */
    public Optional<IOException> getIOException()
    {
        return Optional.ofNullable(m_IOException);
    }

    public String getJobId()
    {
        return m_JobId;
    }
}
