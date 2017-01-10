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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

import org.elasticsearch.xpack.ml.job.AnalysisLimits;

import static org.elasticsearch.xpack.ml.job.process.autodetect.writer.WriterConstants.EQUALS;
import static org.elasticsearch.xpack.ml.job.process.autodetect.writer.WriterConstants.NEW_LINE;

public class AnalysisLimitsWriter {
    /*
     * The configuration fields used in limits.conf
     */
    private static final String MEMORY_STANZA_STR = "[memory]";
    private static final String RESULTS_STANZA_STR = "[results]";
    private static final String MODEL_MEMORY_LIMIT_CONFIG_STR = "modelmemorylimit";
    private static final String MAX_EXAMPLES_LIMIT_CONFIG_STR = "maxexamples";

    private final AnalysisLimits limits;
    private final OutputStreamWriter writer;

    public AnalysisLimitsWriter(AnalysisLimits limits, OutputStreamWriter writer) {
        this.limits = Objects.requireNonNull(limits);
        this.writer = Objects.requireNonNull(writer);
    }

    public void write() throws IOException {
        StringBuilder contents = new StringBuilder(MEMORY_STANZA_STR).append(NEW_LINE);
        if (limits.getModelMemoryLimit() != null && limits.getModelMemoryLimit() != 0L) {
            contents.append(MODEL_MEMORY_LIMIT_CONFIG_STR + EQUALS)
            .append(limits.getModelMemoryLimit()).append(NEW_LINE);
        }

        contents.append(RESULTS_STANZA_STR).append(NEW_LINE);
        if (limits.getCategorizationExamplesLimit() != null) {
            contents.append(MAX_EXAMPLES_LIMIT_CONFIG_STR + EQUALS)
            .append(limits.getCategorizationExamplesLimit())
            .append(NEW_LINE);
        }

        writer.write(contents.toString());
    }
}
