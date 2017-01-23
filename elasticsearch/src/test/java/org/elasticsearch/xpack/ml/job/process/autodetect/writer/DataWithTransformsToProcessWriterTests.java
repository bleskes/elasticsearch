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
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.DataDescription.DataFormat;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.ml.job.config.transform.TransformConfig;
import org.elasticsearch.xpack.ml.job.config.transform.TransformConfigs;
import org.elasticsearch.xpack.ml.job.config.transform.TransformType;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class DataWithTransformsToProcessWriterTests extends ESTestCase {
    private AutodetectProcess autodetectProcess;
    private DataCountsReporter dataCountsReporter;
    private Logger logger;

    private List<String[]> writtenRecords;

    @Before
    public void setUpMocks() throws IOException {
        autodetectProcess = Mockito.mock(AutodetectProcess.class);
        dataCountsReporter = Mockito.mock(DataCountsReporter.class);
        logger = Mockito.mock(Logger.class);

        writtenRecords = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String[] record = (String[]) invocation.getArguments()[0];
                String[] copy = Arrays.copyOf(record, record.length);
                writtenRecords.add(copy);
                return null;
            }
        }).when(autodetectProcess).writeRecord(any(String[].class));
    }

    public void testCsvWriteWithConcat() throws IOException {
        StringBuilder input = new StringBuilder();
        input.append("time,host,metric,value\n");
        input.append("1,hostA,foo,3.0\n");
        input.append("2,hostB,bar,2.0\n");
        input.append("2,hostA,bar,2.0\n");

        InputStream inputStream = createInputStream(input.toString());
        AbstractDataToProcessWriter writer = createWriter(true);
        writer.writeHeader();
        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "concat", "value", "."});
        expectedRecords.add(new String[]{"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[]{"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[]{"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(dataCountsReporter).finishReporting();
    }

    public void testJsonWriteWithConcat() throws IOException {
        StringBuilder input = new StringBuilder();
        input.append("{\"time\" : 1, \"host\" : \"hostA\", \"metric\" : \"foo\", \"value\" : 3.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostB\", \"metric\" : \"bar\", \"value\" : 2.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostA\", \"metric\" : \"bar\", \"value\" : 2.0}\n");

        InputStream inputStream = createInputStream(input.toString());
        AbstractDataToProcessWriter writer = createWriter(false);
        writer.writeHeader();
        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "concat", "value", "."});
        expectedRecords.add(new String[]{"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[]{"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[]{"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(dataCountsReporter).finishReporting();
    }


    private static InputStream createInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private AbstractDataToProcessWriter createWriter(boolean doCsv) {
        DataDescription.Builder dd = new DataDescription.Builder();
        dd.setFieldDelimiter(',');
        dd.setFormat(doCsv ? DataFormat.DELIMITED : DataFormat.JSON);
        dd.setTimeFormat(DataDescription.EPOCH);

        Detector.Builder detector = new Detector.Builder("metric", "value");
        detector.setByFieldName("concat");
        AnalysisConfig ac = new AnalysisConfig.Builder(Arrays.asList(detector.build())).build();

        TransformConfig tc = new TransformConfig(TransformType.Names.CONCAT_NAME);
        tc.setInputs(Arrays.asList("host", "metric"));

        TransformConfigs tcs = new TransformConfigs(Arrays.asList(tc));

        if (doCsv) {
            return new CsvDataToProcessWriter(true, autodetectProcess, dd.build(), ac, tcs, dataCountsReporter, logger);
        } else {
            return new JsonDataToProcessWriter(true, autodetectProcess, dd.build(), ac, tcs, dataCountsReporter, logger);
        }
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords) {
        assertEquals(expectedRecords.size(), writtenRecords.size());
        for (int i = 0; i < expectedRecords.size(); i++) {
            assertArrayEquals(expectedRecords.get(i), writtenRecords.get(i));
        }
    }
}
