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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Logger;

public class JobRouter
{
    private static final Logger LOGGER = Logger.getLogger(JobRouter.class);

    private final List<OutputStream> m_OutputStreams;

    public JobRouter(List<OutputStream> outputStreams)
    {
        m_OutputStreams = outputStreams;
    }

    public void routeToAll(String record)
    {
        byte [] data = record.getBytes(StandardCharsets.UTF_8);
        for (OutputStream out : m_OutputStreams)
        {
            try
            {
                out.write(data);
            }
            catch (IOException e)
            {
                LOGGER.error("Error writing to stream", e);
            }
        }
    }

    public void routeToJob(String partitionFieldValue, String record)
    {
        try
        {
            m_OutputStreams.get(route(partitionFieldValue)).write(record.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing to job", e);
        }
    }

    private int route(String partitionFieldValue)
    {
        return Math.abs(partitionFieldValue.hashCode()) % m_OutputStreams.size();
    }
}
