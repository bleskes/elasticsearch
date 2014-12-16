/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import static org.mockito.Mockito.doAnswer;
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
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.dateparsing.DateTransformer;
import com.prelert.job.process.dateparsing.DoubleDateTransformer;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;

@RunWith(MockitoJUnitRunner.class)
public class CsvDataToProcessWriterTest
{

    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
    private DataDescription m_DataDescription;
    private AnalysisConfig m_AnalysisConfig;
    @Mock private StatusReporter m_StatusReporter;
    @Mock private JobDataPersister m_DataPersister;
    @Mock private Logger m_Logger;
    private DateTransformer m_DateTransformer;

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
        m_DataDescription.setFormat(DataFormat.DELINEATED);
        m_DataDescription.setTimeFormat(DataDescription.EPOCH);

        m_AnalysisConfig = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        m_AnalysisConfig.setDetectors(Arrays.asList(detector));

        m_DateTransformer = new DoubleDateTransformer(false);
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

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "value", "."});
        expectedRecords.add(new String[] {"3", "3.0", ""});
        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter, times(2)).reportOutOfOrderRecord(2);
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
                m_AnalysisConfig, m_StatusReporter, m_DataPersister, m_Logger, m_DateTransformer);
    }

    private void assertWrittenRecordsEqualTo(List<String[]> expectedRecords)
    {
        for (int i = 0; i < expectedRecords.size(); i++)
        {
            assertArrayEquals(expectedRecords.get(i), m_WrittenRecords.get(i));
        }
    }
}
