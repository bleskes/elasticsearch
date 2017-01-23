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
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.ml.job.config.transform.TransformConfigs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * This writer is used for reading inputIndex data that are unstructured and
 * each record is a single line. The writer applies transforms and pipes
 * the records into length encoded outputIndex.
 * <p>
 * This writer is expected only to be used in combination of transforms
 * that will extract the time and the other fields used in the analysis.
 * <p>
 * Records for which no time can be extracted will be ignored.
 */
public class SingleLineDataToProcessWriter extends AbstractDataToProcessWriter {
    private static final String RAW = "raw";

    protected SingleLineDataToProcessWriter(boolean includeControlField, AutodetectProcess autodetectProcess,
                                            DataDescription dataDescription, AnalysisConfig analysisConfig,
                                            TransformConfigs transformConfigs, DataCountsReporter dataCountsReporter, Logger logger) {
        super(includeControlField, autodetectProcess, dataDescription, analysisConfig, transformConfigs, dataCountsReporter, logger);
    }

    @Override
    public DataCounts write(InputStream inputStream) throws IOException {
        dataCountsReporter.startNewIncrementalCount();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] header = {RAW};
            buildTransforms(header);

            int numFields = outputFieldCount();
            String[] record = new String[numFields];

            for (String line = bufferedReader.readLine(); line != null;
                    line = bufferedReader.readLine()) {
                Arrays.fill(record, "");
                applyTransformsAndWrite(new String[]{line}, record, 1);
            }
            dataCountsReporter.finishReporting();
        }

        return dataCountsReporter.incrementalStats();
    }

    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
            Map<String, Integer> inputFieldIndexes, String[] header) {
        return true;
    }
}
