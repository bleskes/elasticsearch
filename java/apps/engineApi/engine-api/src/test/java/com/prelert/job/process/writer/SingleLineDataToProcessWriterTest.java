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
import static org.junit.Assert.assertEquals;

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
public class SingleLineDataToProcessWriterTest
{
    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
    private DataDescription m_DataDescription;
    private AnalysisConfig m_AnalysisConfig;
    private List<TransformConfig> m_TransformConfigs;
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

        m_DataDescription = new DataDescription();
        m_DataDescription.setFieldDelimiter(',');
        m_DataDescription.setFormat(DataFormat.SINGLE_LINE);
        m_DataDescription.setTimeFormat(DataDescription.FORMAT);
        m_DataDescription.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        m_AnalysisConfig = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFunction("count");
        detector.setByFieldName("message");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));

        m_TransformConfigs = new ArrayList<>();
    }

    @Test
    public void testWrite_GivenDataIsValid() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setInputs(Arrays.asList("raw"));
        transformConfig.setOutputs(Arrays.asList("time", "message"));
        transformConfig.setTransform("extract");
        transformConfig.setArguments(Arrays.asList("(.{20}) (.*)"));
        m_TransformConfigs.add(transformConfig);

        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        input.append("2015-04-29 11:00:00Z This is message 2\r");
        input.append("2015-04-29 12:00:00Z This is message 3\r\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).getLatestRecordTime();
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();
        verify(m_StatusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 1430301600000L);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 1430305200000L);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 1430308800000L);
        verify(m_StatusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "message", "."});
        expectedRecords.add(new String[] {"1430301600", "This is message 1", ""});
        expectedRecords.add(new String[] {"1430305200", "This is message 2", ""});
        expectedRecords.add(new String[] {"1430308800", "This is message 3", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
        verifyNoMoreInteractions(m_StatusReporter);
    }

    @Test
    public void testWrite_GivenDataContainsInvalidRecords() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        TransformConfig transformConfig = new TransformConfig();
        transformConfig.setInputs(Arrays.asList("raw"));
        transformConfig.setOutputs(Arrays.asList("time", "message"));
        transformConfig.setTransform("extract");
        transformConfig.setArguments(Arrays.asList("(.{20}) (.*)"));
        m_TransformConfigs.add(transformConfig);

        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        input.append("No transform\n");
        input.append("Transform can apply but no date to be parsed\n");
        input.append("\n");
        input.append("2015-04-29 12:00:00Z This is message 3\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).getLatestRecordTime();
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();
        verify(m_StatusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 1430301600000L);
        verify(m_StatusReporter, times(1)).reportRecordWritten(1, 1430308800000L);
        verify(m_StatusReporter, times(2)).reportFailedTransform();
        verify(m_StatusReporter, times(3)).reportDateParseError(1);
        verify(m_StatusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "message", "."});
        expectedRecords.add(new String[] {"1430301600", "This is message 1", ""});
        expectedRecords.add(new String[] {"1430308800", "This is message 3", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
        verifyNoMoreInteractions(m_StatusReporter);
    }

    @Test
    public void testWrite_GivenNoTransforms() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        StringBuilder input = new StringBuilder();
        input.append("2015-04-29 10:00:00Z This is message 1\n");
        InputStream inputStream = createInputStream(input.toString());
        SingleLineDataToProcessWriter writer = createWriter();

        writer.write(inputStream);
        verify(m_StatusReporter, times(1)).startNewIncrementalCount();
        verify(m_StatusReporter, times(1)).setAnalysedFieldsPerRecord(1);
        verify(m_StatusReporter, times(1)).reportDateParseError(1);
        verify(m_StatusReporter, times(1)).incrementalStats();

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "message", "."});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).getLatestRecordTime();
        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
        verifyNoMoreInteractions(m_StatusReporter);
    }

    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private SingleLineDataToProcessWriter createWriter()
    {
        return new SingleLineDataToProcessWriter(true, m_LengthEncodedWriter, m_DataDescription,
                m_AnalysisConfig, new TransformConfigs(m_TransformConfigs), m_StatusReporter,
                m_DataPersister, m_Logger);
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
