/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.audit.Auditor;
import org.junit.Before;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProblemTrackerTests extends ESTestCase {

    private Auditor auditor;

    private ProblemTracker problemTracker;

    @Before
    public void setUpTests() {
        auditor = mock(Auditor.class);
        problemTracker = new ProblemTracker(() -> auditor);
    }

    public void testReportExtractionProblem() {
        problemTracker.reportExtractionProblem("foo");

        verify(auditor).error("Datafeed is encountering errors extracting data: foo");
        assertTrue(problemTracker.hasProblems());
    }

    public void testReportAnalysisProblem() {
        problemTracker.reportAnalysisProblem("foo");

        verify(auditor).error("Datafeed is encountering errors submitting data for analysis: foo");
        assertTrue(problemTracker.hasProblems());
    }

    public void testReportProblem_GivenSameProblemTwice() {
        problemTracker.reportExtractionProblem("foo");
        problemTracker.reportAnalysisProblem("foo");

        verify(auditor, times(1)).error("Datafeed is encountering errors extracting data: foo");
        assertTrue(problemTracker.hasProblems());
    }

    public void testReportProblem_GivenSameProblemAfterFinishReport() {
        problemTracker.reportExtractionProblem("foo");
        problemTracker.finishReport();
        problemTracker.reportExtractionProblem("foo");

        verify(auditor, times(1)).error("Datafeed is encountering errors extracting data: foo");
        assertTrue(problemTracker.hasProblems());
    }

    public void testUpdateEmptyDataCount_GivenEmptyNineTimes() {
        for (int i = 0; i < 9; i++) {
            problemTracker.updateEmptyDataCount(true);
        }

        Mockito.verifyNoMoreInteractions(auditor);
    }

    public void testUpdateEmptyDataCount_GivenEmptyTenTimes() {
        for (int i = 0; i < 10; i++) {
            problemTracker.updateEmptyDataCount(true);
        }

        verify(auditor).warning("Datafeed has been retrieving no data for a while");
    }

    public void testUpdateEmptyDataCount_GivenEmptyElevenTimes() {
        for (int i = 0; i < 11; i++) {
            problemTracker.updateEmptyDataCount(true);
        }

        verify(auditor, times(1)).warning("Datafeed has been retrieving no data for a while");
    }

    public void testUpdateEmptyDataCount_GivenNonEmptyAfterNineEmpty() {
        for (int i = 0; i < 9; i++) {
            problemTracker.updateEmptyDataCount(true);
        }
        problemTracker.updateEmptyDataCount(false);

        Mockito.verifyNoMoreInteractions(auditor);
    }

    public void testUpdateEmptyDataCount_GivenNonEmptyAfterTenEmpty() {
        for (int i = 0; i < 10; i++) {
            problemTracker.updateEmptyDataCount(true);
        }
        problemTracker.updateEmptyDataCount(false);

        verify(auditor).warning("Datafeed has been retrieving no data for a while");
        verify(auditor).info("Datafeed has started retrieving data again");
    }

    public void testFinishReport_GivenNoProblems() {
        problemTracker.finishReport();

        assertFalse(problemTracker.hasProblems());
        Mockito.verifyNoMoreInteractions(auditor);
    }

    public void testFinishReport_GivenRecovery() {
        problemTracker.reportExtractionProblem("bar");
        problemTracker.finishReport();
        problemTracker.finishReport();

        verify(auditor).error("Datafeed is encountering errors extracting data: bar");
        verify(auditor).info("Datafeed has recovered data extraction and analysis");
        assertFalse(problemTracker.hasProblems());
    }
}
