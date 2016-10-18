
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.supercsv.exception.SuperCsvException;

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
import org.elasticsearch.xpack.prelert.job.transform.TransformType;

public class CsvDataToProcessWriterTest extends ESTestCase {

    @Mock
    private AutodetectProcess autodetectProcess;
    private List<TransformConfig> transforms;
    private DataDescription dataDescription;
    private AnalysisConfig analysisConfig;
    @Mock
    private StatusReporter statusReporter;
    @Mock
    private Logger jobLogger;

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

        transforms = new ArrayList<>();

        dataDescription = new DataDescription();
        dataDescription.setFieldDelimiter(',');
        dataDescription.setFormat(DataFormat.DELIMITED);
        dataDescription.setTimeFormat(DataDescription.EPOCH);

        analysisConfig = new AnalysisConfig();
        Detector detector = new Detector("metric", "value");
        analysisConfig.setDetectors(Arrays.asList(detector));
    }

    public void testWrite_GivenTimeFormatIsEpochAndDataIsValid() throws MissingFieldException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,foo,1.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "value", "."});
        expectedRecords.add(new String[]{"1", "1.0", ""});
        expectedRecords.add(new String[]{"2", "2.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenTransformAndEmptyField() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        TransformConfig transform = new TransformConfig("uppercase");
        transform.setInputs(Arrays.asList("value"));
        transform.setOutputs(Arrays.asList("transformed"));
        transforms.add(transform);

        Detector existingDetector = analysisConfig.getDetectors().get(0);
        Detector newDetector = new Detector(existingDetector.getFunction(), "transformed");
        newDetector.setByFieldName(existingDetector.getByFieldName());
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        newDetector.setDetectorDescription(existingDetector.getDetectorDescription());
        analysisConfig.getDetectors().set(0, newDetector);

        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,,foo\n");
        input.append("2,,\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "transformed", "."});
        expectedRecords.add(new String[]{"1", "FOO", ""});
        expectedRecords.add(new String[]{"2", "", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenTimeFormatIsEpochAndTimestampsAreOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("3,foo,3.0\n");
        input.append("1,bar,2.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "value", "."});
        expectedRecords.add(new String[]{"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter, times(2)).reportOutOfOrderRecord(2);
        verify(statusReporter, never()).reportLatestTimeIncrementalStats(anyLong());
        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenTimeFormatIsEpochAndAllRecordsAreOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,foo,1.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());

        when(statusReporter.getLatestRecordTime()).thenReturn(new Date(5000L));
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "value", "."});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter, times(2)).reportOutOfOrderRecord(2);
        verify(statusReporter, times(2)).reportLatestTimeIncrementalStats(anyLong());
        verify(statusReporter, never()).reportRecordWritten(anyLong(), anyLong());
        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenTimeFormatIsEpochAndSomeTimestampsWithinLatencySomeOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {
        analysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("4,foo,4.0\n");
        input.append("5,foo,5.0\n");
        input.append("3,foo,3.0\n");
        input.append("4,bar,4.0\n");
        input.append("2,bar,2.0\n");
        input.append("\0");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "value", "."});
        expectedRecords.add(new String[]{"4", "4.0", ""});
        expectedRecords.add(new String[]{"5", "5.0", ""});
        expectedRecords.add(new String[]{"3", "3.0", ""});
        expectedRecords.add(new String[]{"4", "4.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter, times(1)).reportOutOfOrderRecord(2);
        verify(statusReporter, never()).reportLatestTimeIncrementalStats(anyLong());
        verify(statusReporter).finishReporting();
    }

    public void testWrite_NullByte()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException {
        analysisConfig.setLatency(0L);

        StringBuilder input = new StringBuilder();
        input.append("metric,value,time\n");
        input.append("foo,4.0,1\n");
        input.append("\0");   // the csv reader skips over this line
        input.append("foo,5.0,2\n");
        input.append("foo,3.0,3\n");
        input.append("bar,4.0,4\n");
        input.append("\0");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "value", "."});
        expectedRecords.add(new String[]{"1", "4.0", ""});
        expectedRecords.add(new String[]{"2", "5.0", ""});
        expectedRecords.add(new String[]{"3", "3.0", ""});
        expectedRecords.add(new String[]{"4", "4.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter, times(1)).reportMissingField();
        verify(statusReporter, times(1)).reportRecordWritten(2, 1000);
        verify(statusReporter, times(1)).reportRecordWritten(2, 2000);
        verify(statusReporter, times(1)).reportRecordWritten(2, 3000);
        verify(statusReporter, times(1)).reportRecordWritten(2, 4000);
        verify(statusReporter, times(1)).reportDateParseError(2);
        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenDateTimeFieldIsOutputOfTransform() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        TransformConfig transform = new TransformConfig("concat");
        transform.setInputs(Arrays.asList("date", "time-of-day"));
        transform.setOutputs(Arrays.asList("datetime"));

        transforms.add(transform);

        dataDescription = new DataDescription();
        dataDescription.setFieldDelimiter(',');
        dataDescription.setTimeField("datetime");
        dataDescription.setFormat(DataFormat.DELIMITED);
        dataDescription.setTimeFormat("yyyy-MM-ddHH:mm:ssX");

        CsvDataToProcessWriter writer = createWriter();

        StringBuilder input = new StringBuilder();
        input.append("date,time-of-day,metric,value\n");
        input.append("1970-01-01,00:00:01Z,foo,5.0\n");
        input.append("1970-01-01,00:00:02Z,foo,6.0\n");
        InputStream inputStream = createInputStream(input.toString());

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"datetime", "value", "."});
        expectedRecords.add(new String[]{"1", "5.0", ""});
        expectedRecords.add(new String[]{"2", "6.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenChainedTransforms_SortsByDependencies() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {
        TransformConfig tc1 = new TransformConfig(TransformType.Names.UPPERCASE_NAME);
        tc1.setInputs(Arrays.asList("dns"));
        tc1.setOutputs(Arrays.asList("dns_upper"));

        TransformConfig tc2 = new TransformConfig(TransformType.Names.CONCAT_NAME);
        tc2.setInputs(Arrays.asList("dns1", "dns2"));
        tc2.setArguments(Arrays.asList("."));
        tc2.setOutputs(Arrays.asList("dns"));

        transforms.add(tc1);
        transforms.add(tc2);

        Detector detector = new Detector("metric", "value");
        detector.setByFieldName("dns_upper");
        analysisConfig.setDetectors(Arrays.asList(detector));

        StringBuilder input = new StringBuilder();
        input.append("time,dns1,dns2,value\n");
        input.append("1,www,foo.com,1.0\n");
        input.append("2,www,bar.com,2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(statusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[]{"time", "dns_upper", "value", "."});
        expectedRecords.add(new String[]{"1", "WWW.FOO.COM", "1.0", ""});
        expectedRecords.add(new String[]{"2", "WWW.BAR.COM", "2.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(statusReporter).finishReporting();
    }

    public void testWrite_GivenMisplacedQuoteMakesRecordExtendOverTooManyLines() throws MissingFieldException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException {

        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,\"foo,1.0\n");
        for (int i = 0; i < 10000 - 1; i++) {
            input.append("\n");
        }
        input.append("2,bar\",2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        SuperCsvException e = ESTestCase.expectThrows(SuperCsvException.class, () -> writer.write(inputStream));
        assertEquals("max number of lines to read exceeded while reading quoted column beginning on line 2 and ending on line 10001",
                e.getMessage());
    }

    private static InputStream createInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private CsvDataToProcessWriter createWriter() {
        return new CsvDataToProcessWriter(true, autodetectProcess, dataDescription,
                analysisConfig, new TransformConfigs(transforms),
                statusReporter, jobLogger);
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords) {
        assertEquals(expectedRecords.size(), writtenRecords.size());
        for (int i = 0; i < expectedRecords.size(); i++) {
            assertArrayEquals(expectedRecords.get(i), writtenRecords.get(i));
        }
    }
}
