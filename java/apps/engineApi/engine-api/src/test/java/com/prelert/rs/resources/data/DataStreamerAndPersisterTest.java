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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.prelert.job.DataCounts;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

public class DataStreamerAndPersisterTest
{

    @Before
    public void setUp()
    {
        givenNoPersistBaseDir();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_GivenNullJobManager()
    {
        new DataStreamerAndPersister(null);
    }

    @Test
    public void testStreamData_GivenNoContentEncodingAndNoPersistBaseDir()
            throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, IOException, MalformedJsonException
    {
        JobManager jobManager = mock(JobManager.class);
        DataStreamerAndPersister dataStreamer = new DataStreamerAndPersister(jobManager);
        InputStream inputStream = mock(InputStream.class);

        when(jobManager.submitDataLoadAndPersistJob("foo", inputStream)).thenReturn(
                new DataCounts());

        dataStreamer.streamData(null, "foo", inputStream);

        verify(jobManager).submitDataLoadAndPersistJob("foo", inputStream);
        Mockito.verifyNoMoreInteractions(jobManager);
    }

    private void givenNoPersistBaseDir()
    {
        System.clearProperty("persistbasedir");
    }
}
