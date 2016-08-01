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

package com.prelert.master.superjob;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Logger;

public class JobRouter implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(JobRouter.class);

    private static final int BUFFER_SIZE = 1024 * 1024;

    private final List<OutputStream> m_OutputStreams;
    private final ByteBuffer [] m_WriteBuffers;

    public JobRouter(List<OutputStream> outputStreams)
    {
        this(outputStreams, BUFFER_SIZE);
    }

    public JobRouter(List<OutputStream> outputStreams, int bufferSize)
    {
        m_OutputStreams = outputStreams;
        m_WriteBuffers = new ByteBuffer[outputStreams.size()];
        createByteBuffers(outputStreams.size(), bufferSize);
    }

    private void createByteBuffers(int numBuffers, int bufferSize)
    {
        for (int i=0; i<numBuffers; i++)
        {
            m_WriteBuffers[i] = ByteBuffer.allocate(bufferSize);
        }
    }

    public void routeToAll(String record)
    {
        byte [] data = record.getBytes(StandardCharsets.UTF_8);
        for (int i=0; i<m_WriteBuffers.length; i++)
        {
            write(i, data);
        }
    }

    public void routeToJob(String partitionFieldValue, String record)
    {
        int route = route(partitionFieldValue);
        byte [] data = record.getBytes(StandardCharsets.UTF_8);
        write(route, data);
    }

    public void flush()
    {
        for (int i=0; i<m_WriteBuffers.length; i++)
        {
            drainBuffer(i);
        }
    }

    private void write(int route, byte [] data)
    {
        if (data.length > m_WriteBuffers[route].remaining())
        {
            drainBuffer(route);
        }
        m_WriteBuffers[route].put(data);
    }

    private void drainBuffer(int route)
    {
        ByteBuffer bb = m_WriteBuffers[route];
        try
        {
            m_OutputStreams.get(route).write(bb.array(), 0, bb.position());
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing to job", e);
        }
        finally
        {
            bb.clear();
        }
    }

    private int route(String partitionFieldValue)
    {
        return Math.abs(partitionFieldValue.hashCode()) % m_OutputStreams.size();
    }

    @Override
    public void close()
    {
        flush();
    }
}
