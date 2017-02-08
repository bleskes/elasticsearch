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
package org.elasticsearch.xpack.ml.job.process.autodetect.writer;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.config.AnalysisLimits;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AnalysisLimitsWriterTests extends ESTestCase {
    private OutputStreamWriter writer;

    @Before
    public void setUpMocks() {
        writer = Mockito.mock(OutputStreamWriter.class);
    }

    @After
    public void verifyNoMoreWriterInteractions() {
        verifyNoMoreInteractions(writer);
    }

    public void testWrite_GivenUnsetValues() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(null, null);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n[results]\n");
    }

    public void testWrite_GivenModelMemoryLimitIsZero() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(0L, null);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n[results]\n");
    }

    public void testWrite_GivenModelMemoryLimitWasSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(10L, null);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\nmodelmemorylimit = 10\n[results]\n");
    }

    public void testWrite_GivenCategorizationExamplesLimitWasSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(0L, 5L);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n[results]\nmaxexamples = 5\n");
    }

    public void testWrite_GivenAllFieldsSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(1024L, 3L);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write(
                "[memory]\nmodelmemorylimit = 1024\n[results]\nmaxexamples = 3\n");
    }
}
