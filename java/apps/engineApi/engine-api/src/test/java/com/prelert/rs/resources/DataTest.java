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

package com.prelert.rs.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.InterimResultsParams;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.provider.RestApiException;


public class DataTest extends ServiceTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private Data m_Data;

    @Before
    public void setUp()
    {
        m_Data = new Data();
        configureService(m_Data);
    }

    @Test
    public void testFlushUpload_GivenNoInterimResultsAndStartSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid interim results parameters.");

        m_Data.flushUpload("foo", false, "1", "");
    }

    @Test
    public void testFlushUpload_GivenNoInterimResultsAndEndSpecified() throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid interim results parameters.");

        m_Data.flushUpload("foo", false, "", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyEndSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid interim results parameters.");

        m_Data.flushUpload("foo", true, "", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndEndIsBeforeStart()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid interim results parameters.");

        m_Data.flushUpload("foo", true, "2", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsEpochs()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "1428494400", "1428498000");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq("foo"), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIso()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "2015-04-08T12:00:00Z", "2015-04-08T13:00:00Z");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq("foo"), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIsoMilliseconds()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "2015-04-08T12:00:00.000Z", "2015-04-08T13:00:00.000Z");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq("foo"), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndSameStartAndEnd()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "1428494400", "1428494400");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq("foo"), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyStartIsSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        JobDetails jobDetails = new JobDetails();
        jobDetails.setAnalysisConfig(analysisConfig);
        SingleDocument<JobDetails> job = new SingleDocument<JobDetails>();
        job.setDocument(jobDetails);

        JobManager jobManager = jobManager();
        when(jobManager.getJob("foo")).thenReturn(job);

        m_Data.flushUpload("foo", true, "1428494400", "");

        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq("foo"), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }
}
