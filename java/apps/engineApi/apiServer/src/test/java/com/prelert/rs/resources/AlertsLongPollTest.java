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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import javax.ws.rs.container.AsyncResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertTrigger;
import com.prelert.job.alert.AlertType;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
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
        m_Alerts.pollJob(JOB_ID, 90, 80.0, 60.0, AlertType.BUCKET.toString(), false, asyncResponse);

        verify(alertManager()).registerRequest(Mockito.same(asyncResponse), Mockito.eq(JOB_ID),
                Mockito.eq(BASE_URI), Mockito.eq(90l), Mockito.any());
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

        m_Alerts.pollJob(JOB_ID, 90, -0.01, null, AlertType.BUCKET.toString(), false,
                            mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, -0.01, 60.0, AlertType.BUCKET.toString(), false,
                mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, 101.0, 60.0, AlertType.BUCKET.toString(), true,
                        mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, null, -0.01, AlertType.BUCKET.toString(), true,
                mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, 90.0, -0.01,AlertType.BUCKET.toString(), true,
                mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, 95.0, 101.0, AlertType.BUCKET.toString(), false,
                mock(AsyncResponse.class));
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

        m_Alerts.pollJob(JOB_ID, 90, null, null,
                AlertType.BUCKET.toString(), true, mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenTimeoutIsLessThanZero() throws UnknownJobException,
            InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Invalid timeout parameter. Timeout must be > 0");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TIMEOUT_ARGUMENT));

        m_Alerts.pollJob(JOB_ID, -1, 10.0, 10.0, AlertType.BUCKET.toString(), false,
                mock(AsyncResponse.class));
    }

    @Test
    public void testPollJob_GivenBadAlertType() throws UnknownJobException,
            InterruptedException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("The alert type argument 'broken' isn't a recognised type");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNKNOWN_ALERT_TYPE));

        m_Alerts.pollJob(JOB_ID, 90, 10.0, 10.0, "broken", false, mock(AsyncResponse.class));
    }

    @Test
    public void testCreateAlertTriggers()
    {
        AlertTrigger [] triggers = m_Alerts.createAlertTriggers(AlertType.BUCKET.toString(),
                                                                0.1, 0.2, true);
        assertEquals(1, triggers.length);
        assertEquals(AlertType.BUCKET, triggers[0].getAlertType());
        assertEquals(0.1, triggers[0].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[0].getNormalisedThreshold(), 0.0001);
        assertTrue(triggers[0].isIncludeInterim());

        triggers = m_Alerts.createAlertTriggers(AlertType.BUCKETINFLUENCER.toString(),
                                                0.1, 0.2, false);
        assertEquals(1, triggers.length);
        assertEquals(AlertType.BUCKETINFLUENCER, triggers[0].getAlertType());
        assertEquals(0.1, triggers[0].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[0].getNormalisedThreshold(), 0.0001);
        assertFalse(triggers[0].isIncludeInterim());

        triggers = m_Alerts.createAlertTriggers(AlertType.INFLUENCER.toString(),
                                                    0.1, null, false);
        assertEquals(1, triggers.length);
        assertEquals(AlertType.INFLUENCER, triggers[0].getAlertType());
        assertEquals(0.1, triggers[0].getAnomalyThreshold(), 0.0001);
        assertEquals(null, triggers[0].getNormalisedThreshold());
        assertFalse(triggers[0].isIncludeInterim());
    }

    @Test
    public void testCreateAlertTriggers_givenMultipleTypes()
    {
        AlertTrigger [] triggers = m_Alerts.createAlertTriggers("bucket,influencer,bucketinfluencer",
                                            0.1, 0.2, true);
        Arrays.sort(triggers,
                (a,b) -> a.getAlertType().toString().compareTo(b.getAlertType().toString()));

        assertEquals(3, triggers.length);
        assertEquals(AlertType.BUCKET, triggers[0].getAlertType());
        assertEquals(0.1, triggers[0].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[0].getNormalisedThreshold(), 0.0001);
        assertTrue(triggers[0].isIncludeInterim());

        assertEquals(AlertType.BUCKETINFLUENCER, triggers[1].getAlertType());
        assertEquals(0.1, triggers[1].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[1].getNormalisedThreshold(), 0.0001);
        assertTrue(triggers[1].isIncludeInterim());

        assertEquals(AlertType.INFLUENCER, triggers[2].getAlertType());
        assertEquals(0.1, triggers[2].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[2].getNormalisedThreshold(), 0.0001);
        assertTrue(triggers[2].isIncludeInterim());
    }

    @Test
    public void testCreateAlertTriggers_givenMultipleSameTypes()
    {
        AlertTrigger [] triggers = m_Alerts.createAlertTriggers("influencer,influencer",
                                            0.1, 0.2, true);

        assertEquals(1, triggers.length);

        assertEquals(AlertType.INFLUENCER, triggers[0].getAlertType());
        assertEquals(0.1, triggers[0].getAnomalyThreshold(), 0.0001);
        assertEquals(0.2, triggers[0].getNormalisedThreshold(), 0.0001);
        assertTrue(triggers[0].isIncludeInterim());
    }


    @Test(expected=RestApiException.class)
    public void testCreateAlertTriggers_throws()
    {
         m_Alerts.createAlertTriggers("blah,blah", 0.1, 0.2, false);
    }
}
