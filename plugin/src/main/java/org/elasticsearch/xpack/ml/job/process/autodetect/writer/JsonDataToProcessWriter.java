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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A writer for transforming and piping JSON data from an
 * inputstream to outputstream.
 * The data written to outputIndex is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
class JsonDataToProcessWriter extends AbstractDataToProcessWriter {

    private static final Logger LOGGER = Loggers.getLogger(JsonDataToProcessWriter.class);
    private NamedXContentRegistry xContentRegistry;

    JsonDataToProcessWriter(boolean includeControlField, AutodetectProcess autodetectProcess,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            DataCountsReporter dataCountsReporter, NamedXContentRegistry xContentRegistry) {
        super(includeControlField, autodetectProcess, dataDescription, analysisConfig,
                dataCountsReporter, LOGGER);
        this.xContentRegistry = xContentRegistry;
    }

    /**
     * Read the JSON inputIndex, transform to length encoded values and pipe to
     * the OutputStream. No transformation is applied to the data the timestamp
     * is expected in seconds from the epoch. If any of the fields in
     * <code>analysisFields</code> or the <code>DataDescription</code>s
     * timeField is missing from the JOSN inputIndex an exception is thrown
     */
    @Override
    public void write(InputStream inputStream, XContentType xContentType, BiConsumer<DataCounts, Exception> handler)
            throws IOException {
        dataCountsReporter.startNewIncrementalCount();

        try (XContentParser parser = XContentFactory.xContent(xContentType)
                .createParser(xContentRegistry, inputStream)) {
            writeJson(parser);

            // this line can throw and will be propagated
            dataCountsReporter.finishReporting(
                    ActionListener.wrap(
                            response -> handler.accept(dataCountsReporter.incrementalStats(), null),
                            e -> handler.accept(null, e)
                    ));
        }
    }

    private void writeJson(XContentParser parser) throws IOException {
        Collection<String> analysisFields = inputFields();

        buildFieldIndexMapping(analysisFields.toArray(new String[0]));

        int numFields = outputFieldCount();
        String[] input = new String[numFields];
        String[] record = new String[numFields];

        // We never expect to get the control field
        boolean[] gotFields = new boolean[analysisFields.size()];

        XContentRecordReader recordReader = new XContentRecordReader(parser, inFieldIndexes,
                LOGGER);
        long inputFieldCount = recordReader.read(input, gotFields);
        while (inputFieldCount >= 0) {
            Arrays.fill(record, "");

            inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count

            long missing = missingFieldCount(gotFields);
            if (missing > 0) {
                dataCountsReporter.reportMissingFields(missing);
            }

            for (InputOutputMap inOut : inputOutputMap) {
                String field = input[inOut.inputIndex];
                record[inOut.outputIndex] = (field == null) ? "" : field;
            }

            transformTimeAndWrite(record, inputFieldCount);

            inputFieldCount = recordReader.read(input, gotFields);
        }
    }

    /**
     * Don't enforce the check that all the fields are present in JSON docs.
     * Always returns true
     */
    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
            Map<String, Integer> inputFieldIndexes,
            String[] header) {
        return true;
    }

    /**
     * Return the number of missing fields
     */
    private static long missingFieldCount(boolean[] gotFieldFlags) {
        long count = 0;

        for (int i = 0; i < gotFieldFlags.length; i++) {
            if (gotFieldFlags[i] == false) {
                ++count;
            }
        }

        return count;
    }
}
