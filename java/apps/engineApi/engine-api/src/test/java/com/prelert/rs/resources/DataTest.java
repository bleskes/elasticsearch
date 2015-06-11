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

import static com.prelert.job.errorcodes.ErrorCodeMatcher.hasErrorCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.provider.RestApiException;


public class DataTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private Data m_Data;
    private AnalysisConfig m_AnalysisConfig;
    private List<Detector> m_Detectors;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Data = new Data();
        m_AnalysisConfig = new AnalysisConfig();
        m_Detectors = new ArrayList<>();
        m_Detectors.add(new Detector());
        m_AnalysisConfig.setDetectors(m_Detectors);
        m_AnalysisConfig.setLatency(null);
        configureService(m_Data);
        SingleDocument<JobDetails> job = new SingleDocument<>();
        JobDetails jobDetails = new JobDetails();
        jobDetails.setAnalysisConfig(m_AnalysisConfig);
        job.setDocument(jobDetails);
        when(jobManager().getJob(JOB_ID)).thenReturn(job);
    }

    @Test
    public void testStreamData() throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        when(jobManager().submitDataLoadJob(eq(JOB_ID), eq(inputStream), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "", "");

        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(JOB_ID), eq(inputStream), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertFalse(params.isResettingBuckets());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    @Test
    public void testStreamData_GivenResetStartAndResetEndSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        when(jobManager().submitDataLoadJob(eq(JOB_ID), eq(inputStream), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());
        givenLatency(3600L);
        givenDetectorsWithFunctions("count");

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "1428592200");

        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(JOB_ID), eq(inputStream), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertTrue(params.isResettingBuckets());
        assertEquals("1428591600", params.getStart());
        assertEquals("1428592200", params.getEnd());
    }

    @Test
    public void testStreamData_GivenNoLatencySpecified() throws UnknownJobException,
    NativeProcessRunException, MissingFieldException, JobInUseException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        givenLatency(null);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Bucket resetting is not supported when no latency is configured.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "");
    }

    @Test
    public void testStreamData_GivenZeroLatencySpecified() throws UnknownJobException,
    NativeProcessRunException, MissingFieldException, JobInUseException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        givenLatency(0L);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Bucket resetting is not supported when no latency is configured.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "");
    }

    @Test
    public void testStreamData_GivenOnlyResetStartSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        when(jobManager().submitDataLoadJob(eq(JOB_ID), eq(inputStream), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());
        givenLatency(3600L);
        givenDetectorsWithFunctions("count");

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "");

        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(JOB_ID), eq(inputStream), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertTrue(params.isResettingBuckets());
        assertEquals("1428591600", params.getStart());
        assertEquals("1428591601", params.getEnd());
    }

    @Test
    public void testStreamData_GivenSameResetStartAndResetEndSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        when(jobManager().submitDataLoadJob(eq(JOB_ID), eq(inputStream), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());
        givenLatency(3600L);
        givenDetectorsWithFunctions("mean");

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "1428591600");

        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(JOB_ID), eq(inputStream), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertTrue(params.isResettingBuckets());
        assertEquals("1428591600", params.getStart());
        assertEquals("1428591601", params.getEnd());
    }

    @Test
    public void testStreamData_GivenResetEndIsBeforeResetStart() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid time range: end time '1428591599' is earlier than start time '1428591600'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591600", "1428591599");
    }

    @Test
    public void testStreamData_GivenOnlyResetEndSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid reset range parameters: 'resetStart' has not been specified.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_BUCKET_RESET_RANGE_PARAMS));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "", "1428591599");
    }

    @Test
    public void testStreamData_GivenInvalidResetStartSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Query param 'resetStart' with value 'not a date' cannot"
                + " be parsed as a date or converted to a number (epoch)");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "not a date", "1428591599");
    }

    @Test
    public void testStreamData_GivenInvalidResetEndSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, MalformedJsonException, IOException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Query param 'resetEnd' with value 'not a date' cannot"
                + " be parsed as a date or converted to a number (epoch)");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "1428591599", "not a date");
    }

    @Test
    public void testFlushUpload_GivenNoInterimResultsAndStartSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: unexpected 'start' and/or 'end'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, false, "1", "");
    }

    @Test
    public void testFlushUpload_GivenNoInterimResultsAndEndSpecified() throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: unexpected 'start' and/or 'end'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, false, "", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyEndSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: 'start' has not been specified.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, true, "", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndEndIsBeforeStart()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Invalid time range: end time '1' is earlier than start time '2'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));

        m_Data.flushUpload(JOB_ID, true, "2", "1");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsEpochs()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "1428494400", "1428498000");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIso()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "2015-04-08T12:00:00Z", "2015-04-08T13:00:00Z");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIsoMilliseconds()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "2015-04-08T12:00:00.000Z", "2015-04-08T13:00:00.000Z");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndSameStartAndEnd()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "1428494400", "1428494400");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyStartIsSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "1428494400", "");

        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager()).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculate());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    private void givenLatency(Long latency)
    {
        m_AnalysisConfig.setLatency(latency);
    }

    private void givenDetectorsWithFunctions(String... functions)
    {
        m_Detectors.clear();
        for (String function : functions)
        {
            Detector detector = new Detector();
            detector.setFunction(function);
            m_Detectors.add(detector);
        }
    }
}
