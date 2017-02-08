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

import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import static org.elasticsearch.xpack.ml.job.process.autodetect.writer.WriterConstants.EQUALS;
import static org.elasticsearch.xpack.ml.job.process.autodetect.writer.WriterConstants.NEW_LINE;

public class ModelDebugConfigWriter {

    private final ModelDebugConfig modelDebugConfig;
    private final Writer writer;

    public ModelDebugConfigWriter(ModelDebugConfig modelDebugConfig, Writer writer) {
        this.modelDebugConfig = Objects.requireNonNull(modelDebugConfig);
        this.writer = Objects.requireNonNull(writer);
    }

    public void write() throws IOException {
        StringBuilder contents = new StringBuilder();

        contents.append("boundspercentile")
                .append(EQUALS)
                .append(modelDebugConfig.getBoundsPercentile())
                .append(NEW_LINE);

        String terms = modelDebugConfig.getTerms();
        contents.append(ModelDebugConfig.TERMS_FIELD.getPreferredName())
                .append(EQUALS)
                .append(terms == null ? "" : terms)
                .append(NEW_LINE);

        writer.write(contents.toString());
    }
}
