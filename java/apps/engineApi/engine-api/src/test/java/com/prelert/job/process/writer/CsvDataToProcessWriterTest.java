/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
import static org.junit.Assert.assertEquals;
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
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;

@RunWith(MockitoJUnitRunner.class)
public class CsvDataToProcessWriterTest
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
        m_DataDescription.setFieldDelimiter(',');
        m_DataDescription.setFormat(DataFormat.DELIMITED);
        m_DataDescription.setTimeFormat(DataDescription.EPOCH);

        m_AnalysisConfig = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndDataIsValid() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,foo,1.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

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
    public void testWrite_GivenTransformAndEmptyField() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        TransformConfig transform = new TransformConfig();
        transform.setTransform("uppercase");
        transform.setInputs(Arrays.asList("value"));
        transform.setOutputs(Arrays.asList("transformed"));
        m_Transforms.add(transform);

        m_AnalysisConfig.getDetectors().get(0).setFieldName("transformed");

        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,,foo\n");
        input.append("2,,\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "transformed", "."});
        expectedRecords.add(new String[] {"1", "FOO", ""});
        expectedRecords.add(new String[] {"2", "", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndTimestampsAreOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException
    {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("3,foo,3.0\n");
        input.append("1,bar,2.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(2)).reportOutOfOrderRecord(2);
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndAllRecordsAreOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException
    {
        StringBuilder input = new StringBuilder();
        input.append("time,metric,value\n");
        input.append("1,foo,1.0\n");
        input.append("2,bar,2.0\n");
        InputStream inputStream = createInputStream(input.toString());

        when(m_StatusReporter.getLatestRecordTime()).thenReturn(new Date(5000L));
        CsvDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(2)).reportOutOfOrderRecord(2);
        verify(m_StatusReporter, never()).reportRecordWritten(anyLong(), anyLong());
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_GivenTimeFormatIsEpochAndSomeTimestampsWithinLatencySomeOutOfOrder()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException
    {
        m_AnalysisConfig.setLatency(2L);

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
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"4", "4.0", ""});
        expectedRecords.add(new String[] {"5", "5.0", ""});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        expectedRecords.add(new String[] {"4", "4.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(1)).reportOutOfOrderRecord(2);
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testWrite_NullByte()
            throws MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, IOException
    {
        m_AnalysisConfig.setLatency(0L);

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
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"1", "4.0", ""});
        expectedRecords.add(new String[] {"2", "5.0", ""});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        expectedRecords.add(new String[] {"4", "4.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(1)).reportMissingField();
        verify(m_StatusReporter, times(1)).reportRecordWritten(2, 1);
        verify(m_StatusReporter, times(1)).reportRecordWritten(2, 2);
        verify(m_StatusReporter, times(1)).reportRecordWritten(2, 3);
        verify(m_StatusReporter, times(1)).reportRecordWritten(2, 4);
        verify(m_StatusReporter, times(1)).reportDateParseError(2);
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private CsvDataToProcessWriter createWriter()
    {
        return new CsvDataToProcessWriter(m_LengthEncodedWriter, m_DataDescription,
                m_AnalysisConfig, new TransformConfigs(m_Transforms),
                m_StatusReporter, m_DataPersister, m_Logger);
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords)
    {
        assertEquals(expectedRecords.size(), m_WrittenRecords.size());
        for (int i = 0; i < expectedRecords.size(); i++)
        {
            assertArrayEquals(expectedRecords.get(i), m_WrittenRecords.get(i));
        }
    }
}
