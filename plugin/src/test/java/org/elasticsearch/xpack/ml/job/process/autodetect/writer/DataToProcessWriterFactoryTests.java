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
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.DataDescription.DataFormat;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;

import static org.mockito.Mockito.mock;

public class DataToProcessWriterFactoryTests extends ESTestCase {
    public void testCreate_GivenDataFormatIsJson() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.JSON);

        assertTrue(createWriter(dataDescription.build()) instanceof JsonDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsCsv() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.DELIMITED);

        assertTrue(createWriter(dataDescription.build()) instanceof CsvDataToProcessWriter);
    }

    private static DataToProcessWriter createWriter(DataDescription dataDescription) {
        return DataToProcessWriterFactory.create(true, mock(AutodetectProcess.class), dataDescription,
                mock(AnalysisConfig.class), mock(DataCountsReporter.class));
    }
}
