/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.process.writer;

import static org.junit.Assert.assertArrayEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.transform.TransformType;

@RunWith(MockitoJUnitRunner.class)
public class JsonDataToProcessWriterTest
{

    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
    private List<TransformConfig> m_Transforms;
    private DataDescription m_DataDescription;
    private AnalysisConfig m_AnalysisConfig;
    @Mock private StatusReporter m_StatusReporter;
    @Mock private JobDataPersister m_DataPersister;
    @Mock private Logger m_Logger;

    private List<String[]> m_WrittenRecords;

    @Before
    public void setUp() throws IOException
    {
        m_WrittenRecords = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                String[] record = (String[]) invocation.getArguments()[0];
                String[] copy = Arrays.copyOf(record, record.length);
                m_WrittenRecords.add(copy);
                return null;
            }
        }).when(m_LengthEncodedWriter).writeRecord(any(String[].class));

        m_Transforms = new ArrayList<>();

        m_DataDescription = new DataDescription();
        m_DataDescription.setFormat(DataFormat.JSON);
        m_DataDescription.setTimeFormat(DataDescription.EPOCH);

        m_AnalysisConfig = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndDataIsValid() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException
    {
        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"metric\":\"foo\", \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\", \"metric\":\"bar\", \"value\":\"2.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"1", "1.0", ""});
        expectedRecords.add(new String[] {"2", "2.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndTimestampsAreOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"3\", \"metric\":\"foo\", \"value\":\"3.0\"}");
        input.append("{\"time\":\"1\", \"metric\":\"bar\", \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\", \"metric\":\"bar\", \"value\":\"2.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(2)).reportOutOfOrderRecord(2);
        verify(m_StatusReporter, never()).reportLatestTimeIncrementalStats(anyLong());
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndSomeTimestampsWithinLatencySomeOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        m_AnalysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"4\", \"metric\":\"foo\", \"value\":\"4.0\"}");
        input.append("{\"time\":\"5\", \"metric\":\"foo\", \"value\":\"5.0\"}");
        input.append("{\"time\":\"3\", \"metric\":\"bar\", \"value\":\"3.0\"}");
        input.append("{\"time\":\"4\", \"metric\":\"bar\", \"value\":\"4.0\"}");
        input.append("{\"time\":\"2\", \"metric\":\"bar\", \"value\":\"2.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"4", "4.0", ""});
        expectedRecords.add(new String[] {"5", "5.0", ""});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        expectedRecords.add(new String[] {"4", "4.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(1)).reportOutOfOrderRecord(2);
        verify(m_StatusReporter, never()).reportLatestTimeIncrementalStats(anyLong());
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenMalformedJsonWithoutNestedLevels()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        m_AnalysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\" \"value\":\"2.0\"}");
        input.append("{\"time\":\"3\", \"value\":\"3.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"1", "1.0", ""});
        expectedRecords.add(new String[] {"2", "", ""});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).reportMissingFields(1);
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenMalformedJsonWithNestedLevels()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        Detector detector = new Detector();
        detector.setFieldName("nested.value");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));
        m_AnalysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"nested\":{\"value\":\"1.0\"}}");
        input.append("{\"time\":\"2\", \"nested\":{\"value\":\"2.0\"} \"foo\":\"bar\"}");
        input.append("{\"time\":\"3\", \"nested\":{\"value\":\"3.0\"}}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "nested.value", "."});
        expectedRecords.add(new String[] {"1", "1.0", ""});
        expectedRecords.add(new String[] {"2", "2.0", ""});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test (expected = MalformedJsonException.class)
    public void testWrite_GivenMalformedJsonThatNeverRecovers()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        m_AnalysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"value\":\"2.0\"}");
        input.append("{\"time");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
    }

    @Test
    public void testWrite_GivenJsonWithArrayField()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        m_AnalysisConfig.setLatency(2L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"array\":[\"foo\", \"bar\"], \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\", \"array\":[], \"value\":\"2.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"1", "1.0", ""});
        expectedRecords.add(new String[] {"2", "2.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenJsonWithMissingFields()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException, MalformedJsonException
    {
        m_AnalysisConfig.setLatency(0L);

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"f1\":\"foo\", \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\", \"value\":\"2.0\"}");
        input.append("{\"time\":\"3\", \"f1\":\"bar\"}");
        input.append("{}");
        input.append("{\"time\":\"4\", \"value\":\"3.0\"}");

        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"1", "1.0", ""});
        expectedRecords.add(new String[] {"2", "2.0", ""});
        expectedRecords.add(new String[] {"3", "", ""});
        expectedRecords.add(new String[] {"4", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(1)).reportMissingFields(1L);
        verify(m_StatusReporter, times(1)).reportRecordWritten(2, 1000);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 2000);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 3000);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 4000);
        verify(m_StatusReporter, times(1)).reportDateParseError(0);
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenDateTimeFieldIsOutputOfTransform() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException
    {
        TransformConfig transform = new TransformConfig();
        transform.setTransform("concat");
        transform.setInputs(Arrays.asList("date", "time-of-day"));
        transform.setOutputs(Arrays.asList("datetime"));

        m_Transforms.add(transform);

        m_DataDescription = new DataDescription();
        m_DataDescription.setFieldDelimiter(',');
        m_DataDescription.setTimeField("datetime");
        m_DataDescription.setFormat(DataFormat.DELIMITED);
        m_DataDescription.setTimeFormat("yyyy-MM-ddHH:mm:ssX");

        JsonDataToProcessWriter writer = createWriter();

        StringBuilder input = new StringBuilder();
        input.append("{\"date\":\"1970-01-01\", \"time-of-day\":\"00:00:01Z\", \"value\":\"5.0\"}");
        input.append("{\"date\":\"1970-01-01\", \"time-of-day\":\"00:00:02Z\", \"value\":\"6.0\"}");
        InputStream inputStream = createInputStream(input.toString());

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"datetime", "value", "."});
        expectedRecords.add(new String[] {"1", "5.0", ""});
        expectedRecords.add(new String[] {"2", "6.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenChainedTransforms_SortsByDependencies() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException
    {
        TransformConfig tc1 = new TransformConfig();
        tc1.setTransform(TransformType.Names.UPPERCASE_NAME);
        tc1.setInputs(Arrays.asList("dns"));
        tc1.setOutputs(Arrays.asList("dns_upper"));

        TransformConfig tc2 = new TransformConfig();
        tc2.setTransform(TransformType.Names.CONCAT_NAME);
        tc2.setInputs(Arrays.asList("dns1", "dns2"));
        tc2.setArguments(Arrays.asList("."));
        tc2.setOutputs(Arrays.asList("dns"));

        m_Transforms.add(tc1);
        m_Transforms.add(tc2);

        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName("dns_upper");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));

        StringBuilder input = new StringBuilder();
        input.append("{\"time\":\"1\", \"dns1\":\"www\", \"dns2\":\"foo.com\", \"value\":\"1.0\"}");
        input.append("{\"time\":\"2\", \"dns1\":\"www\", \"dns2\":\"bar.com\", \"value\":\"2.0\"}");
        InputStream inputStream = createInputStream(input.toString());
        JsonDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "dns_upper", "value", "."});
        expectedRecords.add(new String[] {"1", "WWW.FOO.COM","1.0", ""});
        expectedRecords.add(new String[] {"2", "WWW.BAR.COM", "2.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }


    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private JsonDataToProcessWriter createWriter()
    {
        return new JsonDataToProcessWriter(true, m_LengthEncodedWriter, m_DataDescription,
                m_AnalysisConfig, null, new TransformConfigs(m_Transforms),
                m_StatusReporter, m_DataPersister, m_Logger);
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords)
    {
        for (int i = 0; i < expectedRecords.size(); i++)
        {
            assertArrayEquals(expectedRecords.get(i), m_WrittenRecords.get(i));
        }
    }
}
