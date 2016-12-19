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

public class DataToProcessWriterFactoryTests extends ESTestCase {
    public void testCreate_GivenDataFormatIsJson() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.JSON);

        assertTrue(createWriter(dataDescription.build()) instanceof JsonDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsElasticsearch() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);

        assertTrue(createWriter(dataDescription.build()) instanceof JsonDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsCsv() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.DELIMITED);

        assertTrue(createWriter(dataDescription.build()) instanceof CsvDataToProcessWriter);
    }

    public void testCreate_GivenDataFormatIsSingleLine() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataFormat.SINGLE_LINE);

        assertTrue(createWriter(dataDescription.build()) instanceof SingleLineDataToProcessWriter);
    }

    private static DataToProcessWriter createWriter(DataDescription dataDescription) {
        return DataToProcessWriterFactory.create(true, mock(AutodetectProcess.class), dataDescription,
                mock(AnalysisConfig.class), mock(TransformConfigs.class), mock(StatusReporter.class), mock(Logger.class));
    }
}
