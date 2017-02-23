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
import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

    public void testWrite_GivenEnabledConfigWithoutTerms() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("boundspercentile = 95.0\nterms = \n");
    }

    public void testWrite_GivenEnabledConfigWithTerms() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(true, "foo,bar");
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("boundspercentile = 95.0\nterms = foo,bar\n");
    }

    public void testWrite_GivenDisabledConfigWithTerms() throws IOException {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(false, "foo,bar");
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, this.writer);

        writer.write();

        verify(this.writer).write("boundspercentile = -1.0\nterms = foo,bar\n");
    }
}
