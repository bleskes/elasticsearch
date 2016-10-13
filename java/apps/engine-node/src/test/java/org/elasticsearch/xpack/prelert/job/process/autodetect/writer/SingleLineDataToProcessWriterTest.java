
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.elasticsearch.mock.orig.Mockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;

public class SingleLineDataToProcessWriterTest extends ESTestCase {
    @Mock
    private AutodetectProcess autodetectProcess;
    private DataDescription dataDescription;
    private AnalysisConfig analysisConfig;
    private List<TransformConfig> transformConfigs;
    @Mock
    private StatusReporter statusReporter;

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

        dataDescription = new DataDescription();
        dataDescription.setFieldDelimiter(',');
        dataDescription.setFormat(DataFormat.SINGLE_LINE);
        dataDescription.setTimeFormat(DataDescription.FORMAT);
        dataDescription.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        analysisConfig = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFunction("count");
        detector.setByFieldName("message");
        analysisConfig.setDetectors(Arrays.asList(detector));

        transformConfigs = new ArrayList<>();
    }

    public void testWrite_GivenDataIsValid() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setInputs(Arrays.asList("raw"));
        transformConfig.setOutputs(Arrays.asList("time", "message"));
        transformConfig.setTransform("extract");
        transformConfig.setArguments(Arrays.asList("(.{20}) (.*)"));
        transformConfigs.add(transformConfig);

        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        input.append("2015-04-29 11:00:00Z This is message 2\r");
        input.append("2015-04-29 12:00:00Z This is message 3\r\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).getLatestRecordTime();
        verify(statusReporter, times(1)).startNewIncrementalCount();
        verify(statusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(statusReporter, times(1)).reportRecordWritten(1, 1430301600000L);
        verify(statusReporter, times(1)).reportRecordWritten(1, 1430305200000L);
        verify(statusReporter, times(1)).reportRecordWritten(1, 1430308800000L);
        verify(statusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "message", "."});
        expectedRecords.add(new String[]{"1430301600", "This is message 1", ""});
        expectedRecords.add(new String[]{"1430305200", "This is message 2", ""});
        expectedRecords.add(new String[]{"1430308800", "This is message 3", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
        verifyNoMoreInteractions(statusReporter);
    }

    public void testWrite_GivenDataContainsInvalidRecords() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setInputs(Arrays.asList("raw"));
        transformConfig.setOutputs(Arrays.asList("time", "message"));
        transformConfig.setTransform("extract");
        transformConfig.setArguments(Arrays.asList("(.{20}) (.*)"));
        transformConfigs.add(transformConfig);

        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        input.append("No transform\n");
        input.append("Transform can apply but no date to be parsed\n");
        input.append("\n");
        input.append("2015-04-29 12:00:00Z This is message 3\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).getLatestRecordTime();
        verify(statusReporter, times(1)).startNewIncrementalCount();
        verify(statusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(statusReporter, times(1)).reportRecordWritten(1, 1430301600000L);
        verify(statusReporter, times(1)).reportRecordWritten(1, 1430308800000L);
        verify(statusReporter, times(2)).reportFailedTransform();
        verify(statusReporter, times(3)).reportDateParseError(1);
        verify(statusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "message", "."});
        expectedRecords.add(new String[]{"1430301600", "This is message 1", ""});
        expectedRecords.add(new String[]{"1430308800", "This is message 3", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
        verifyNoMoreInteractions(statusReporter);
    }

    public void testWrite_GivenNoTransforms() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();
        verify(statusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(statusReporter, times(1)).reportDateParseError(1);
        verify(statusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "message", "."});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).getLatestRecordTime();
        verify(statusReporter).finishReporting();
        verifyNoMoreInteractions(statusReporter);
    }

    private static InputStream createInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private SingleLineDataToProcessWriter createWriter() {
        return new SingleLineDataToProcessWriter(true, autodetectProcess, dataDescription,
                analysisConfig, new TransformConfigs(transformConfigs), statusReporter, mock(Logger.class));
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords) {
        assertEquals(expectedRecords.size(), writtenRecords.size());
        for (int i = 0; i < expectedRecords.size(); i++) {
            assertArrayEquals(expectedRecords.get(i), writtenRecords.get(i));
        }
    }
}
