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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig;
import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig.DebugDestination;

public class ModelDebugConfigWriterTests extends ESTestCase {
    private OutputStreamWriter writer;

    @Before
    public void setUpMocks() {
        writer = Mockito.mock(OutputStreamWriter.class);
    }

    @After
    public void verifyNoMoreWriterInteractions() {
        verifyNoMoreInteractions(writer);
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
