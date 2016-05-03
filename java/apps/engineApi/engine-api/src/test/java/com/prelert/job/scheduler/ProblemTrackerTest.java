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

package com.prelert.job.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.audit.Auditor;

public class ProblemTrackerTest
{
    @Mock private Auditor m_Auditor;

    private ProblemTracker m_ProblemTracker;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ProblemTracker = new ProblemTracker(() -> m_Auditor);
    }

    @Test
    public void testReportExtractionProblem()
    {
        m_ProblemTracker.reportExtractionProblem("foo");

        verify(m_Auditor).error("Scheduler is encountering errors extracting data: foo");
        assertTrue(m_ProblemTracker.hasProblems());
    }

    @Test
    public void testReportAnalysisProblem()
    {
        m_ProblemTracker.reportAnalysisProblem("foo");

        verify(m_Auditor).error("Scheduler is encountering errors submitting data for analysis: foo");
        assertTrue(m_ProblemTracker.hasProblems());
    }

    @Test
    public void testReportProblem_GivenSameProblemTwice()
    {
        m_ProblemTracker.reportExtractionProblem("foo");
        m_ProblemTracker.reportAnalysisProblem("foo");

        verify(m_Auditor, times(1)).error("Scheduler is encountering errors extracting data: foo");
        assertTrue(m_ProblemTracker.hasProblems());
    }

    @Test
    public void testReportProblem_GivenSameProblemAfterFinishReport()
    {
        m_ProblemTracker.reportExtractionProblem("foo");
        m_ProblemTracker.finishReport();
        m_ProblemTracker.reportExtractionProblem("foo");

        verify(m_Auditor, times(1)).error("Scheduler is encountering errors extracting data: foo");
        assertTrue(m_ProblemTracker.hasProblems());
    }

    @Test
    public void testUpdateEmptyDataCount_GivenEmptyNineTimes()
    {
        for (int i = 0; i < 9; i++)
        {
            m_ProblemTracker.updateEmptyDataCount(true);
        }

        Mockito.verifyNoMoreInteractions(m_Auditor);
    }

    @Test
    public void testUpdateEmptyDataCount_GivenEmptyTenTimes()
    {
        for (int i = 0; i < 10; i++)
        {
            m_ProblemTracker.updateEmptyDataCount(true);
        }

        verify(m_Auditor).warning("Scheduler has been retrieving no data for a while");
    }

    @Test
    public void testUpdateEmptyDataCount_GivenEmptyElevenTimes()
    {
        for (int i = 0; i < 11; i++)
        {
            m_ProblemTracker.updateEmptyDataCount(true);
        }

        verify(m_Auditor, times(1)).warning("Scheduler has been retrieving no data for a while");
    }

    @Test
    public void testUpdateEmptyDataCount_GivenNonEmptyAfterNineEmpty()
    {
        for (int i = 0; i < 9; i++)
        {
            m_ProblemTracker.updateEmptyDataCount(true);
        }
        m_ProblemTracker.updateEmptyDataCount(false);

        Mockito.verifyNoMoreInteractions(m_Auditor);
    }

    @Test
    public void testUpdateEmptyDataCount_GivenNonEmptyAfterTenEmpty()
    {
        for (int i = 0; i < 10; i++)
        {
            m_ProblemTracker.updateEmptyDataCount(true);
        }
        m_ProblemTracker.updateEmptyDataCount(false);

        verify(m_Auditor).warning("Scheduler has been retrieving no data for a while");
        verify(m_Auditor).info("Scheduler has started retrieving data again");
    }

    @Test
    public void testFinishReport_GivenNoProblems()
    {
        m_ProblemTracker.finishReport();

        assertFalse(m_ProblemTracker.hasProblems());
        Mockito.verifyNoMoreInteractions(m_Auditor);
    }

    @Test
    public void testFinishReport_GivenRecovery()
    {
        m_ProblemTracker.reportExtractionProblem("bar");
        m_ProblemTracker.finishReport();
        m_ProblemTracker.finishReport();

        verify(m_Auditor).error("Scheduler is encountering errors extracting data: bar");
        verify(m_Auditor).info("Scheduler has recovered data extraction and analysis");
        assertFalse(m_ProblemTracker.hasProblems());
    }
}
