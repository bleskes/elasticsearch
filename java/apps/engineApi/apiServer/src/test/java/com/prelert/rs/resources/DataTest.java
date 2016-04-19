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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.DataPostResponse;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.exception.ActionNotAllowedForScheduledJobException;
import com.prelert.rs.exception.InvalidParametersException;
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
        JobDetails jobDetails = new JobDetails();
        jobDetails.setAnalysisConfig(m_AnalysisConfig);

        when(jobManager().getJobOrThrowIfUnknown(JOB_ID)).thenReturn(jobDetails);
    }

    @Test
    public void testStreamData_GivenSingleJob() throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException, IOException,
            TooManyJobsException
    {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);
        when(jobManager().submitDataLoadJob(eq(JOB_ID), eq(inputStream), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());

        Response response = m_Data.streamData(httpHeaders, JOB_ID, inputStream, "", "");

        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(JOB_ID), eq(inputStream), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertFalse(params.isResettingBuckets());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());

        assertEquals(202, response.getStatus());
        MultiDataPostResult result = (MultiDataPostResult) response.getEntity();
        List<DataPostResponse> responses = result.getResponses();
        assertEquals(1, responses.size());
        assertEquals(JOB_ID, responses.get(0).getJobId());
    }

    @Test
    public void testStreamData_GivenMultipleJobs() throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException, IOException,
            TooManyJobsException
    {
        String job1 = "job_1";
        String job2 = "job_2";
        when(jobManager().submitDataLoadJob(eq(job1), any(InputStream.class), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());
        when(jobManager().submitDataLoadJob(eq(job2), any(InputStream.class), any(DataLoadParams.class)))
                .thenReturn(new DataCounts());
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        String input = "";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        Response response = m_Data.streamData(httpHeaders, "job_1,job_2", inputStream, "", "");

        // Job 1
        ArgumentCaptor<DataLoadParams> paramsCaptor = ArgumentCaptor.forClass(DataLoadParams.class);
        verify(jobManager()).submitDataLoadJob(eq(job1), any(InputStream.class), paramsCaptor.capture());
        DataLoadParams params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertFalse(params.isResettingBuckets());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());

        // Job 2
        verify(jobManager()).submitDataLoadJob(eq(job2), any(InputStream.class), paramsCaptor.capture());
        params = paramsCaptor.getValue();
        assertFalse(params.isPersisting());
        assertFalse(params.isResettingBuckets());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());

        // Response
        assertEquals(202, response.getStatus());
        MultiDataPostResult result = (MultiDataPostResult) response.getEntity();
        List<DataPostResponse> responses = result.getResponses();
        assertEquals(2, responses.size());
        assertEquals(job1, responses.get(0).getJobId());
        assertEquals(job2, responses.get(1).getJobId());
    }

    @Test
    public void testStreamData_GivenResetStartAndResetEndSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            LicenseViolationException, MalformedJsonException, IOException, TooManyJobsException
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
    public void testStreamData_GivenNoLatencySpecified()
    throws InvalidParametersException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Bucket resetting is not supported when no latency is configured.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED));

        m_Data.checkLatencyIsNonZero(null);
    }

    @Test
    public void testStreamData_GivenZeroLatencySpecified()
    throws InvalidParametersException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Bucket resetting is not supported when no latency is configured.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED));

        m_Data.checkLatencyIsNonZero(0L);
    }

    @Test
    public void testStreamData_GivenOnlyResetStartSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            LicenseViolationException, MalformedJsonException, IOException, TooManyJobsException
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
            LicenseViolationException, MalformedJsonException, IOException, TooManyJobsException
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
    public void testStreamData_GivenResetEndIsBeforeResetStart() throws InvalidParametersException
    {
        String msg = Messages.getMessage(Messages.REST_START_AFTER_END, "1428591599", "1428591600");

        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(msg);
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));


        m_Data.createTimeRange(AbstractDataLoad.RESET_START_PARAM, "1428591600",
                                AbstractDataLoad.RESET_END_PARAM, "1428591599");
    }

    @Test
    public void testStreamData_GivenOnlyResetEndSpecified() throws InvalidParametersException
    {
        String msg = Messages.getMessage(Messages.REST_INVALID_RESET_PARAMS, AbstractDataLoad.RESET_START_PARAM);

        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(msg);
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_BUCKET_RESET_RANGE_PARAMS));

        m_Data.createDataLoadParams("", "1428591599");
    }

    @Test
    public void testStreamData_GivenInvalidResetStartSpecified() throws UnknownJobException,
            NativeProcessRunException, MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException, IOException
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
            MalformedJsonException, IOException
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
    public void testFlushUpload_GivenOnlyStartSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: unexpected 'start'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, false, "1", "", "");
    }

    @Test
    public void testFlushUpload_GivenOnlyEndSpecified() throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: unexpected 'end'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, false, "", "1", "");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyEndSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid flush parameters: 'start' has not been specified.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.INVALID_FLUSH_PARAMS));

        m_Data.flushUpload(JOB_ID, true, "", "1", "");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndEndIsBeforeStart()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid time range: end time '1' is earlier than start time '2'.");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));

        m_Data.flushUpload(JOB_ID, true, "2", "1", "");
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsEpochs()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "1428494400", "1428498000", "");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIso()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "2015-04-08T12:00:00Z", "2015-04-08T13:00:00Z", "");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsIsoMilliseconds()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "2015-04-08T12:00:00.000Z", "2015-04-08T13:00:00.000Z", "");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndSameStartAndEnd()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "1428494400", "1428494400", "");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInterimResultsAndOnlyStartSpecified()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "1428494400", "", "");

        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager()).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    @Test
    public void testFlushUpload_GivenInvalidAdvanceTime()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Query param 'advanceTime' with value 'not a date' cannot be parsed as a date or converted to a number (epoch).");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_Data.flushUpload("foo", false, "", "", "not a date");
    }

    @Test
    public void testFlushUpload_GivenValidAdvanceTime()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, false, "", "", "2015-04-08T13:00:00.000Z");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertFalse(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1428498000L, params.getAdvanceTime());
    }

    @Test
    public void testFlushUpload_GivenCalcInterimAndAdvanceTime()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload(JOB_ID, true, "", "", "3600");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(3600L, params.getAdvanceTime());
    }

    @Test
    public void testFlushUpload_GivenCalcInterimWithTimeRangeAndAdvanceTime()
            throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        m_Data.flushUpload("foo", true, "150", "300", "200");

        JobManager jobManager = jobManager();
        ArgumentCaptor<InterimResultsParams> paramsCaptor = ArgumentCaptor.forClass(InterimResultsParams.class);
        verify(jobManager).flushJob(eq(JOB_ID), paramsCaptor.capture());

        InterimResultsParams params = paramsCaptor.getValue();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("150", params.getStart());
        assertEquals("300", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(200L, params.getAdvanceTime());
    }

    @Test
    public void testCommitUpload() throws UnknownJobException, NativeProcessRunException,
            JobInUseException
    {
        Response response = m_Data.commitUpload(JOB_ID);

        verify(jobManager()).closeJob(JOB_ID);
        assertEquals(200, response.getStatus());
        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
    }

    @Test
    public void testStreamData_GivenScheduledJob() throws IOException
    {
        when(jobManager().isScheduledJob(JOB_ID)).thenReturn(true);

        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        InputStream inputStream = mock(InputStream.class);

        m_ExpectedException.expect(ActionNotAllowedForScheduledJobException.class);
        m_ExpectedException.expectMessage("This action is not allowed for a scheduled job");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));

        m_Data.streamData(httpHeaders, JOB_ID, inputStream, "", "");
    }

    @Test
    public void testFlushUpload_GivenScheduledJob() throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        when(jobManager().isScheduledJob(JOB_ID)).thenReturn(true);

        m_ExpectedException.expect(ActionNotAllowedForScheduledJobException.class);
        m_ExpectedException.expectMessage("This action is not allowed for a scheduled job");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));

        m_Data.flushUpload(JOB_ID, false, "", "", "");
    }

    @Test
    public void testCommitUpload_GivenScheduledJob() throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        when(jobManager().isScheduledJob(JOB_ID)).thenReturn(true);

        m_ExpectedException.expect(ActionNotAllowedForScheduledJobException.class);
        m_ExpectedException.expectMessage("This action is not allowed for a scheduled job");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));

        m_Data.commitUpload(JOB_ID);
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
