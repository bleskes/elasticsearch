package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;

public class DataWithTransformsToProcessWriterTest extends ESTestCase {
    @Mock
    private AutodetectProcess autodetectProcess;
    @Mock
    private StatusReporter statusReporter;
    @Mock
    private Logger logger;

    private List<String[]> writtenRecords;

    @Before
    public void setUpMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

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

    public void testCsvWriteWithConcat() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException {
        StringBuilder input = new StringBuilder();
        input.append("time,host,metric,value\n");
        input.append("1,hostA,foo,3.0\n");
        input.append("2,hostB,bar,2.0\n");
        input.append("2,hostA,bar,2.0\n");

        InputStream inputStream = createInputStream(input.toString());
        AbstractDataToProcessWriter writer = createWriter(true);

        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "concat", "value", "."});
        expectedRecords.add(new String[]{"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[]{"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[]{"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }

    public void testJsonWriteWithConcat() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException {
        StringBuilder input = new StringBuilder();
        input.append("{\"time\" : 1, \"host\" : \"hostA\", \"metric\" : \"foo\", \"value\" : 3.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostB\", \"metric\" : \"bar\", \"value\" : 2.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostA\", \"metric\" : \"bar\", \"value\" : 2.0}\n");

        InputStream inputStream = createInputStream(input.toString());
        AbstractDataToProcessWriter writer = createWriter(false);

        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "concat", "value", "."});
        expectedRecords.add(new String[]{"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[]{"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[]{"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }


    private static InputStream createInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private AbstractDataToProcessWriter createWriter(boolean doCsv) {
        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setFormat(doCsv ? DataFormat.DELIMITED : DataFormat.JSON);
        dd.setTimeFormat(DataDescription.EPOCH);

        AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName("concat");
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig tc = new TransformConfig();
        tc.setInputs(Arrays.asList("host", "metric"));
        tc.setTransform(TransformType.Names.CONCAT_NAME);

        TransformConfigs tcs = new TransformConfigs(Arrays.asList(tc));

        if (doCsv) {
            return new CsvDataToProcessWriter(true, autodetectProcess, dd, ac, tcs, statusReporter, logger);
        } else {
            return new JsonDataToProcessWriter(true, autodetectProcess, dd, ac, null, tcs, statusReporter, logger);
        }
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords) {
        assertEquals(expectedRecords.size(), writtenRecords.size());
        for (int i = 0; i < expectedRecords.size(); i++) {
            assertArrayEquals(expectedRecords.get(i), writtenRecords.get(i));
        }
    }
}
