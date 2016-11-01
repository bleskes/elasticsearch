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

import static org.mockito.Mockito.mock;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

public class DataToProcessWriterFactoryTests extends ESTestCase {
    public void testCreate_GivenDataFormatIsJson() {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.JSON);

        assertTrue(createWriter(dataDescription) instanceof JsonDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsElasticsearch() {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);

        assertTrue(createWriter(dataDescription) instanceof JsonDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsCsv() {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.DELIMITED);

        assertTrue(createWriter(dataDescription) instanceof CsvDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsSingleLine() {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.SINGLE_LINE);

        assertTrue(createWriter(dataDescription) instanceof SingleLineDataToProcessWriter);
    }

    private static DataToProcessWriter createWriter(DataDescription dataDescription) {
        return DataToProcessWriterFactory.create(true, mock(AutodetectProcess.class), dataDescription,
                mock(AnalysisConfig.class), mock(SchedulerConfig.class), mock(TransformConfigs.class),
                mock(StatusReporter.class), mock(Logger.class));
    }
}
