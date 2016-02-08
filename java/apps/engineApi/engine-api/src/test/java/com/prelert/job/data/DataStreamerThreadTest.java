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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.DataCounts;
import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.process.params.DataLoadParams;

public class DataStreamerThreadTest
{
    private static final String JOB_ID = "foo";
    private static final String CONTENT_ENCODING = "application/json";

    @Mock private DataStreamer m_DataStreamer;
    @Mock private DataLoadParams m_Params;
    @Mock private InputStream m_InputStream;

    private DataStreamerThread m_DataStreamerThread;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_DataStreamerThread = new DataStreamerThread(m_DataStreamer, JOB_ID, CONTENT_ENCODING,
                m_Params, m_InputStream);
    }

    @After
    public void tearDown() throws IOException
    {
        verify(m_InputStream).close();
    }

    @Test
    public void testRun() throws Exception
    {
        DataCounts counts = new DataCounts();
        counts.setBucketCount(42L);
        when(m_DataStreamer.streamData(CONTENT_ENCODING, JOB_ID, m_InputStream, m_Params))
                .thenReturn(counts);

        m_DataStreamerThread.run();

        assertEquals(JOB_ID, m_DataStreamerThread.getJobId());
        assertEquals(counts, m_DataStreamerThread.getDataCounts());
        assertFalse(m_DataStreamerThread.getIOException().isPresent());
        assertFalse(m_DataStreamerThread.getJobException().isPresent());
    }

    @Test
    public void testRun_GivenIOException() throws Exception
    {
        when(m_DataStreamer.streamData(CONTENT_ENCODING, JOB_ID, m_InputStream, m_Params))
                .thenThrow(new IOException("prelert"));

        m_DataStreamerThread.run();

        assertEquals(JOB_ID, m_DataStreamerThread.getJobId());
        assertNull(m_DataStreamerThread.getDataCounts());
        assertEquals("prelert", m_DataStreamerThread.getIOException().get().getMessage());
        assertFalse(m_DataStreamerThread.getJobException().isPresent());
    }

    @Test
    public void testRun_GivenJobException() throws Exception
    {
        when(m_DataStreamer.streamData(CONTENT_ENCODING, JOB_ID, m_InputStream, m_Params))
                .thenThrow(new JobException("job failed", ErrorCodes.JOB_ID_TAKEN));

        m_DataStreamerThread.run();

        assertEquals(JOB_ID, m_DataStreamerThread.getJobId());
        assertNull(m_DataStreamerThread.getDataCounts());
        assertFalse(m_DataStreamerThread.getIOException().isPresent());
        assertEquals("job failed", m_DataStreamerThread.getJobException().get().getMessage());
    }
}
