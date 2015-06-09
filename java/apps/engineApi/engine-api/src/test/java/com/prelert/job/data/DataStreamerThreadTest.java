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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.DataCounts;
import com.prelert.rs.data.ErrorCode;

public class DataStreamerThreadTest
{
    @Test
    public void test_successReturnsDataCounts()
    throws UnknownJobException, NativeProcessRunException,
    MissingFieldException, JobInUseException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    TooManyJobsException, MalformedJsonException, IOException
    {
        DataStreamer streamer = mock(DataStreamer.class);
        DataLoadParams params = mock(DataLoadParams.class);
        InputStream input = mock(InputStream.class);

        DataCounts counts = new DataCounts();
        counts.setBucketCount(100L);

        when(streamer.streamData("", "foo", input, params)).thenReturn(counts);

        DataStreamerThread th = new DataStreamerThread(streamer, "foo", "", params, input);

        th.run();

        assertNotNull(th.getDataCounts());
        assertEquals(counts, th.getDataCounts());
        assertFalse(th.getIOException().isPresent());
        assertFalse(th.getJobException().isPresent());
    }

    @Test
    public void test_jobExceptionThrown()
    throws UnknownJobException, NativeProcessRunException,
    MissingFieldException, JobInUseException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    TooManyJobsException, MalformedJsonException, IOException
    {
        DataStreamer streamer = mock(DataStreamer.class);
        DataLoadParams params = mock(DataLoadParams.class);
        InputStream input = mock(InputStream.class);

        DataCounts counts = new DataCounts();
        counts.setBucketCount(100L);

        when(streamer.streamData("", "foo", input, params))
                    .thenThrow(new UnknownJobException("foo"));

        DataStreamerThread th = new DataStreamerThread(streamer, "foo", "", params, input);

        th.run();

        assertNull(th.getDataCounts());
        assertFalse(th.getIOException().isPresent());
        assertTrue(th.getJobException().isPresent());

        assertEquals(ErrorCode.MISSING_JOB_ERROR, th.getJobException().get().getErrorCode());

        String msg = Messages.getMessage(Messages.JOB_UNKNOWN_ID, "foo");
        assertEquals(msg, th.getJobException().get().getMessage());
    }


    @Test
    public void test_ioExceptionThrown()
    throws UnknownJobException, NativeProcessRunException,
    MissingFieldException, JobInUseException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    TooManyJobsException, MalformedJsonException, IOException
    {
        DataStreamer streamer = mock(DataStreamer.class);
        DataLoadParams params = mock(DataLoadParams.class);
        InputStream input = mock(InputStream.class);

        DataCounts counts = new DataCounts();
        counts.setBucketCount(100L);

        when(streamer.streamData("", "foo", input, params))
                    .thenThrow(new IOException("io error"));

        DataStreamerThread th = new DataStreamerThread(streamer, "foo", "", params, input);

        th.run();

        assertNull(th.getDataCounts());
        assertTrue(th.getIOException().isPresent());
        assertFalse(th.getJobException().isPresent());
        assertEquals("io error", th.getIOException().get().getMessage());
    }

}
