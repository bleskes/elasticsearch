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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.data.DataStreamer;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.provider.RestApiException;

public class DataStreamerTest
{

    @Before
    public void setUp()
    {
        givenNoPersistBaseDir();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_GivenNullJobManager()
    {
        new DataStreamer(null);
    }

    @Test
    public void testStreamData_GivenNoContentEncodingAndNoPersistBaseDir()
            throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, IOException, MalformedJsonException, JobException
    {
        JobManager jobManager = mock(JobManager.class);
        DataStreamer dataStreamer = new DataStreamer(jobManager);
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);

        when(jobManager.submitDataLoadJob("foo", inputStream, params)).thenReturn(
                new DataCounts());

        dataStreamer.streamData(null, "foo", inputStream, params);

        verify(jobManager).submitDataLoadJob("foo", inputStream, params);
        Mockito.verifyNoMoreInteractions(jobManager);
    }


    @Test
    public void testStreamData_ExpectsGzipButNotCompressed()
    throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException,
            IOException, JobException
    {
        JobManager jobManager = mock(JobManager.class);
        DataStreamer dataStreamer = new DataStreamer(jobManager);
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);

        try
        {
            dataStreamer.streamData("gzip", "foo", inputStream, params);
            fail("content encoding : gzip with uncompressed data should throw");
        }
        catch (RestApiException e)
        {
            assertEquals(ErrorCodes.UNCOMPRESSED_DATA, e.getErrorCode());
        }
    }

    @Test
    public void testStreamData_ExpectsGzipUsesGZipStream()
    throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException,
            IOException, JobException
    {
        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
        try (GZIPOutputStream gzip = new GZIPOutputStream(pipedOut))
        {
            gzip.write("Hello World compressed".getBytes(StandardCharsets.UTF_8));

            JobManager jobManager = mock(JobManager.class);
            DataStreamer dataStreamer = new DataStreamer(jobManager);
            DataLoadParams params = mock(DataLoadParams.class);

            when(jobManager.submitDataLoadJob(Mockito.anyString(),
                                            Mockito.any(InputStream.class),
                                            Mockito.any(DataLoadParams.class)))
                          .thenReturn(new DataCounts());

            dataStreamer.streamData("gzip", "foo", pipedIn, params);

            // submitDataLoadJob should be called with a GZIPInputStream
            ArgumentCaptor<InputStream> streamArg = ArgumentCaptor.forClass(InputStream.class);

            verify(jobManager).submitDataLoadJob(Mockito.anyString(),
                                                streamArg.capture(),
                                                Mockito.any(DataLoadParams.class));

            assertTrue(streamArg.getValue() instanceof GZIPInputStream);
        }
    }

    private void givenNoPersistBaseDir()
    {
        System.clearProperty("persistbasedir");
    }
}
