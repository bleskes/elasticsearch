
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

/**
 * Factory for creating the suitable writer depending on
 * whether the data format is JSON or not, and on the kind
 * of date transformation that should occur.
 */
public class DataToProcessWriterFactory {

    /**
     * Constructs a {@link DataToProcessWriter} depending on
     * the data format and the time transformation.
     *
     * @return A {@link JsonDataToProcessWriter} if the data
     * format is JSON or otherwise a {@link CsvDataToProcessWriter}
     */
    public DataToProcessWriter create(boolean includeControlField, RecordWriter writer,
                                      DataDescription dataDescription, AnalysisConfig analysisConfig,
                                      SchedulerConfig schedulerConfig, TransformConfigs transforms,
                                      StatusReporter statusReporter, Logger logger) {
        switch (dataDescription.getFormat()) {
            case JSON:
            case ELASTICSEARCH:
                return new JsonDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        schedulerConfig, transforms, statusReporter, logger);
            case DELIMITED:
                return new CsvDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        transforms, statusReporter, logger);
            case SINGLE_LINE:
                return new SingleLineDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        transforms, statusReporter, logger);
            default:
                throw new IllegalArgumentException();
        }
    }
}
