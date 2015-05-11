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

package com.prelert.job.alert.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;

public class AlertListenerTest
{
    private static final double ERROR = 0.0000001;

    private static final URI BASE_URI = UriBuilder.fromUri("http://testing").build();

    @Mock private JobProvider m_JobProvider;
    @Mock private JobManager m_JobManager;
    private AlertManager m_AlertManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_AlertManager = new AlertManager(m_JobProvider, m_JobManager);
    }

    @Test
    public void testEvaluate_GivenScoreIsEqualToThreshold()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 80.0, 50.0, BASE_URI);
        assertTrue(alertListener.evaluate(80.0, 40.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 80.0, null, BASE_URI);
        assertTrue(alertListener.evaluate(80.0, 40.0));
    }

    @Test
    public void testEvaluate_GivenScoreIsGreaterThanThreshold()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 80.0, 50.0, BASE_URI);
        assertTrue(alertListener.evaluate(80.1, 40.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 80.0, null, BASE_URI);
        assertTrue(alertListener.evaluate(80.1, 40.0));
    }

    @Test
    public void testEvaluate_GivenProbabilityIsEqualToThreshold()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 40.0, 60.0, BASE_URI);
        assertTrue(alertListener.evaluate(30.0, 60.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, 60.0, BASE_URI);
        assertTrue(alertListener.evaluate(30.0, 60.0));
    }

    @Test
    public void testEvaluate_GivenProbabilityIsGreaterThanThreshold()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 40.0, 60.0, BASE_URI);
        assertTrue(alertListener.evaluate(30.0, 60.1));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, 60.0, BASE_URI);
        assertTrue(alertListener.evaluate(30.0, 60.1));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityGreaterThanThresholds()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 40.0, 60.0, BASE_URI);

        assertTrue(alertListener.evaluate(90.0, 90.0));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityBelowThresholds()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 40.0, 60.0, BASE_URI);

        assertFalse(alertListener.evaluate(30.0, 50.0));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityAreNotSet()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, null, BASE_URI);

        assertFalse(alertListener.evaluate(30.0, 50.0));
    }

    @Test
    public void testIsAnomalyScoreAlert()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 50.0, null, BASE_URI);
        assertTrue(alertListener.isAnomalyScoreAlert(51.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 50.0, 40.0, BASE_URI);
        assertTrue(alertListener.isAnomalyScoreAlert(51.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", 50.0, 40.0, BASE_URI);
        assertFalse(alertListener.isAnomalyScoreAlert(49.0));

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, 40.0, BASE_URI);
        assertFalse(alertListener.isAnomalyScoreAlert(49.0));
    }

    @Test
    public void testGetNormalizedProbThreshold()
    {
        AlertListener alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, null, BASE_URI);
        assertEquals(101.0, alertListener.getNormalisedProbThreshold(), ERROR);

        alertListener = new AlertListener(mock(AsyncResponse.class), m_AlertManager,
                "foo", null, 23.4, BASE_URI);
        assertEquals(23.4, alertListener.getNormalisedProbThreshold(), ERROR);
    }
}
