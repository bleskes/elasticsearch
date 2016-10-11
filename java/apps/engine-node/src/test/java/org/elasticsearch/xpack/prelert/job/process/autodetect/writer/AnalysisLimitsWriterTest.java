
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.elasticsearch.xpack.prelert.job.AnalysisLimits;

public class AnalysisLimitsWriterTest extends ESTestCase {
    @Mock
    private OutputStreamWriter writer;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void verifyNoMoreWriterInteractions() {
        verifyNoMoreInteractions(writer);
    }

    public void testWrite_GivenUnsetValues() throws IOException {
        AnalysisLimits limits = new AnalysisLimits();
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n[results]\n");
    }

    public void testWrite_GivenModelMemoryLimitWasSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(10);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\nmodelmemorylimit = 10\n[results]\n");
    }

    public void testWrite_GivenCategorizationExamplesLimitWasSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setCategorizationExamplesLimit(5L);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n[results]\nmaxexamples = 5\n");
    }

    public void testWrite_GivenAllFieldsSet() throws IOException {
        AnalysisLimits limits = new AnalysisLimits(1024, 3L);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write(
                "[memory]\nmodelmemorylimit = 1024\n[results]\nmaxexamples = 3\n");
    }
}
