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
import org.elasticsearch.xpack.ml.job.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.DataDescription;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.ml.job.status.StatusReporter;
import org.elasticsearch.xpack.ml.job.transform.TransformConfigs;

/**
 * Factory for creating the suitable writer depending on
 * whether the data format is JSON or not, and on the kind
 * of date transformation that should occur.
 */
public final class DataToProcessWriterFactory {

    private DataToProcessWriterFactory() {

    }

    /**
     * Constructs a {@link DataToProcessWriter} depending on
     * the data format and the time transformation.
     *
     * @return A {@link JsonDataToProcessWriter} if the data
     * format is JSON or otherwise a {@link CsvDataToProcessWriter}
     */
    public static DataToProcessWriter create(boolean includeControlField, AutodetectProcess autodetectProcess,
                                             DataDescription dataDescription, AnalysisConfig analysisConfig,
                                             TransformConfigs transforms, StatusReporter statusReporter, Logger logger) {
        switch (dataDescription.getFormat()) {
        case JSON:
            return new JsonDataToProcessWriter(includeControlField, autodetectProcess, dataDescription, analysisConfig,
                    transforms, statusReporter, logger);
        case DELIMITED:
            return new CsvDataToProcessWriter(includeControlField, autodetectProcess, dataDescription, analysisConfig,
                    transforms, statusReporter, logger);
        case SINGLE_LINE:
            return new SingleLineDataToProcessWriter(includeControlField, autodetectProcess, dataDescription, analysisConfig,
                    transforms, statusReporter, logger);
        default:
            throw new IllegalArgumentException();
        }
    }
}
