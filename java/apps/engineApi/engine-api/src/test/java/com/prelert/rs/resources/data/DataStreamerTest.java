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

package com.prelert.rs.resources.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

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
            TooManyJobsException, IOException
    {
        JobManager jobManager = mock(JobManager.class);
        DataStreamer dataStreamer = new DataStreamer(jobManager);
        HttpHeaders httpHeaders = createHttpHeadersWithoutContentEncoding();
        InputStream inputStream = mock(InputStream.class);

        dataStreamer.streamData(httpHeaders, "foo", inputStream);

        verify(jobManager).submitDataLoadJob("foo", inputStream);
        Mockito.verifyNoMoreInteractions(jobManager);
    }

    private void givenNoPersistBaseDir()
    {
        System.clearProperty("persistbasedir");
    }

    private HttpHeaders createHttpHeadersWithoutContentEncoding()
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getHeaderString(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);
        return httpHeaders;
    }
}
