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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.ws.rs.container.AsyncResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.rs.data.ErrorCodeMatcher;
import com.prelert.rs.provider.RestApiException;

public class AlertsLongPollTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private AlertsLongPoll m_Alerts;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Alerts = new AlertsLongPoll();
        configureService(m_Alerts);
    }

    @Test
    public void testPollJob_GivenValidParams() throws UnknownJobException, InterruptedException
    {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        m_Alerts.pollJob(JOB_ID, 90, 80.0, 60.0, asyncResponse);

        verify(alertManager()).registerRequest(asyncResponse, JOB_ID, BASE_URI, 90, 80.0, 60.0);
    }

    @Test
    public void testPollJob_GivenAnomalyScoreLessThanZero()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " score (-0.01) must be in the range 0-100");
        m_ExpectedException.expect(

                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));
        m_Alerts.pollJob(JOB_ID, 90, -0.01, null, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenAnomalyScoreLessThanZeroAndValidNormalizedProbability()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " score (-0.01) and probability (60.0) must be in the range 0-100");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, -0.01, 60.0, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenAnomalyScoreGreaterThan100AndValidNormalizedProbability()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " score (101.0) and probability (60.0) must be in the range 0-100");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, 101.0, 60.0, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenNormalizedProbabilityLessThanZero()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " probability (-0.01) must be in the range 0-100");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, null, -0.01, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenValidAnomalyScoreAndNormalizedProbabilityLessThanZero()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " score (90.0) and probability (-0.01) must be in the range 0-100");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, 90.0, -0.01, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenValidAnomalyScoreAndNormalizedProbabilityGreaterThan100()
            throws UnknownJobException, InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid alert parameters."
                + " score (95.0) and probability (101.0) must be in the range 0-100");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, 95.0, 101.0, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenScoreAndProbabilityAreNotSet() throws UnknownJobException,
            InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage(
                "Missing argument: either 'score' or 'probability' must be specified");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_THRESHOLD_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, 90, null, null, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenTimeoutIsLessThanZero() throws UnknownJobException,
            InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid timeout parameter. Timeout must be > 0");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TIMEOUT_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, -1, 10.0, 10.0, mock(AsyncResponse.class));
    }
}
