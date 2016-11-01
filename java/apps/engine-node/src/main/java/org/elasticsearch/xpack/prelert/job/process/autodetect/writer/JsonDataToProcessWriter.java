/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

/**
 * A writer for transforming and piping JSON data from an
 * inputstream to outputstream.
 * The data written to outputIndex is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
class JsonDataToProcessWriter extends AbstractDataToProcessWriter {
    private static final String ELASTICSEARCH_SOURCE_FIELD = "_source";
    private static final String ELASTICSEARCH_FIELDS_FIELD = "fields";

    /**
     * Scheduler config.  May be <code>null</code>.
     */
    private SchedulerConfig schedulerConfig;

    public JsonDataToProcessWriter(boolean includeControlField, AutodetectProcess autodetectProcess,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            SchedulerConfig schedulerConfig, TransformConfigs transforms,
            StatusReporter statusReporter, Logger logger) {
        super(includeControlField, autodetectProcess, dataDescription, analysisConfig, transforms,
                statusReporter, logger);
        this.schedulerConfig = schedulerConfig;
    }

    /**
     * Read the JSON inputIndex, transform to length encoded values and pipe
     * to the OutputStream.
     * No transformation is applied to the data the timestamp is expected
     * in seconds from the epoch.
     * If any of the fields in <code>analysisFields</code> or the
     * <code>DataDescription</code>s timeField is missing from the JOSN inputIndex
     * a <code>MissingFieldException</code> is thrown
     * @throws MissingFieldException                  If any fields are missing from the JSON records
     * @throws HighProportionOfBadTimestampsException If a large proportion
     *                                                of the records read have missing fields
     */
    @Override
    public DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException, MalformedJsonException {
        statusReporter.startNewIncrementalCount();

        try (JsonParser parser = new JsonFactory().createParser(inputStream)) {
            writeJson(parser);

            // this line can throw and will be propagated
            statusReporter.finishReporting();
        }

        return statusReporter.incrementalStats();
    }

    private void writeJson(JsonParser parser) throws IOException, MissingFieldException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    MalformedJsonException {
        Collection<String> analysisFields = inputFields();

        buildTransformsAndWriteHeader(analysisFields.toArray(new String[0]));

        int numFields = outputFieldCount();
        String[] input = new String[numFields];
        String[] record = new String[numFields];

        // We never expect to get the control field
        boolean[] gotFields = new boolean[analysisFields.size()];

        JsonRecordReader recordReader = makeRecordReader(parser);
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

            applyTransformsAndWrite(input, record, inputFieldCount);

            inputFieldCount = recordReader.read(input, gotFields);
        }
    }

    private String getRecordHoldingField() {
        if (dataDescription.getFormat().equals(DataFormat.ELASTICSEARCH)) {
            if (schedulerConfig != null) {
                if (schedulerConfig.getAggregationsOrAggs() != null) {
                    return SchedulerConfig.AGGREGATIONS.getPreferredName();
                }
                if (!Boolean.TRUE.equals(schedulerConfig.getRetrieveWholeSource())) {
                    return ELASTICSEARCH_FIELDS_FIELD;
                }
            }
            return ELASTICSEARCH_SOURCE_FIELD;
        }
        return "";
    }

    private JsonRecordReader makeRecordReader(JsonParser parser) {
        List<String> nestingOrder = (schedulerConfig != null) ?
                schedulerConfig.buildAggregatedFieldList() : Collections.emptyList();
                return nestingOrder.isEmpty() ? new SimpleJsonRecordReader(parser, inFieldIndexes, getRecordHoldingField(), logger)
                        : new AggregatedJsonRecordReader(parser, inFieldIndexes, getRecordHoldingField(), logger, nestingOrder);
    }

    /**
     * Don't enforce the check that all the fields are present in JSON docs.
     * Always returns true
     */
    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
            Map<String, Integer> inputFieldIndexes,
            String[] header)
                    throws MissingFieldException {
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
