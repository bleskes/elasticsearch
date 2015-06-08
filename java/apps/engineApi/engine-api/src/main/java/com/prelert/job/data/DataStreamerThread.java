/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import com.prelert.job.DataCounts;
import com.prelert.job.exceptions.JobException;
import com.prelert.job.process.params.DataLoadParams;

public class DataStreamerThread extends Thread
{
    private DataCounts m_Stats;
    final private String m_JobId;
    final private String  m_ContentEncoding;
    final private DataLoadParams m_Params;
    final private InputStream m_Input;
    final private DataStreamer m_DataStreamer;
    private JobException m_JobException;
    private IOException m_IOException;

    public DataStreamerThread(DataStreamer dataStreamer,
                            String jobId,
                            String  contentEncoding,
                            DataLoadParams params,
                            InputStream input)
    {
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
        return Optional.<JobException>ofNullable(m_JobException);
    }

    /**
     * If an IOException was thrown during the run of this thread it
     * is accessed here. Only call this method after the thread has joined.
     * @return
     */
    public Optional<IOException> getIOException()
    {
        return Optional.<IOException>ofNullable(m_IOException);
    }
}
