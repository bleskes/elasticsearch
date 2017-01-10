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
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A writer for transforming and piping JSON data from an
 * inputstream to outputstream.
 * The data written to outputIndex is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
class JsonDataToProcessWriter extends AbstractDataToProcessWriter {

    public JsonDataToProcessWriter(boolean includeControlField, AutodetectProcess autodetectProcess, DataDescription dataDescription,
                                   AnalysisConfig analysisConfig, TransformConfigs transforms, StatusReporter statusReporter,
                                   Logger logger) {
        super(includeControlField, autodetectProcess, dataDescription, analysisConfig, transforms, statusReporter, logger);
    }

    /**
     * Read the JSON inputIndex, transform to length encoded values and pipe to
     * the OutputStream. No transformation is applied to the data the timestamp
     * is expected in seconds from the epoch. If any of the fields in
     * <code>analysisFields</code> or the <code>DataDescription</code>s
     * timeField is missing from the JOSN inputIndex an exception is thrown
     */
    @Override
    public DataCounts write(InputStream inputStream, Supplier<Boolean> cancelled) throws IOException {
        statusReporter.startNewIncrementalCount();

        try (JsonParser parser = new JsonFactory().createParser(inputStream)) {
            writeJson(parser, cancelled);

            // this line can throw and will be propagated
            statusReporter.finishReporting();
        }

        return statusReporter.incrementalStats();
    }

    private void writeJson(JsonParser parser, Supplier<Boolean> cancelled) throws IOException {
        Collection<String> analysisFields = inputFields();

        buildTransforms(analysisFields.toArray(new String[0]));

        int numFields = outputFieldCount();
        String[] input = new String[numFields];
        String[] record = new String[numFields];

        // We never expect to get the control field
        boolean[] gotFields = new boolean[analysisFields.size()];

        JsonRecordReader recordReader = new SimpleJsonRecordReader(parser, inFieldIndexes, logger);
        long inputFieldCount = recordReader.read(input, gotFields);
        while (inputFieldCount >= 0) {
            Arrays.fill(record, "");

            inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count

            long missing = missingFieldCount(gotFields);
            if (missing > 0) {
                statusReporter.reportMissingFields(missing);
            }

            for (InputOutputMap inOut : inputOutputMap) {
                String field = input[inOut.inputIndex];
                record[inOut.outputIndex] = (field == null) ? "" : field;
            }

            applyTransformsAndWrite(cancelled, input, record, inputFieldCount);

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
