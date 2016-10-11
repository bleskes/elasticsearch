
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

import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig.DebugDestination;

public class ModelDebugConfigWriterTest extends ESTestCase {
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

    public void testWrite_GivenEmptyConfig() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("boundspercentile = null\nterms = \n");
    }

    public void testWrite_GivenFileConfig() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(65.0, "foo,bar");
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("writeto = FILE\nboundspercentile = 65.0\nterms = foo,bar\n");
    }

    public void testWrite_GivenFullConfig() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(DebugDestination.DATA_STORE, 65.0, "foo,bar");
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("writeto = DATA_STORE\nboundspercentile = 65.0\nterms = foo,bar\n");
    }

}
