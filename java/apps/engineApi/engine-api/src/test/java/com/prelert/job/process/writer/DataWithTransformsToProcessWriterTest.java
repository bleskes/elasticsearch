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
public class DataWithTransformsToProcessWriterTest
{
    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
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
    }

    @Test
    public void testCsvWriteWithConcat() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException
    {
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
        expectedRecords.add(new String[] {"time", "concat", "value", "."});
        expectedRecords.add(new String[] {"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[] {"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[] {"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }

    @Test
    public void testJsonWriteWithConcat() throws MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException,
            MalformedJsonException
    {
        StringBuilder input = new StringBuilder();
        input.append("{\"time\" : 1, \"host\" : \"hostA\", \"metric\" : \"foo\", \"value\" : 3.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostB\", \"metric\" : \"bar\", \"value\" : 2.0}\n");
        input.append("{\"time\" : 2, \"host\" : \"hostA\", \"metric\" : \"bar\", \"value\" : 2.0}\n");

        InputStream inputStream = createInputStream(input.toString());
        AbstractDataToProcessWriter writer = createWriter(false);

        writer.write(inputStream);

        List<String[]> expectedRecords = new ArrayList<>();
        // The final field is the control field
        expectedRecords.add(new String[] {"time", "concat", "value", "."});
        expectedRecords.add(new String[] {"1", "hostAfoo", "3.0", ""});
        expectedRecords.add(new String[] {"2", "hostBbar", "2.0", ""});
        expectedRecords.add(new String[] {"2", "hostAbar", "2.0", ""});

        assertWrittenRecordsEqualTo(expectedRecords);

        verify(m_StatusReporter).finishReporting();
        verify(m_DataPersister).flushRecords();
    }


    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private AbstractDataToProcessWriter createWriter(boolean doCsv)
    {
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

        if (doCsv)
        {
            return new CsvDataToProcessWriter(true, m_LengthEncodedWriter, dd,
                ac, tcs,
                m_StatusReporter, m_DataPersister, m_Logger);
        }
        else
        {
            return new JsonDataToProcessWriter(true, m_LengthEncodedWriter, dd,
                    ac, null, tcs,
                    m_StatusReporter, m_DataPersister, m_Logger);
        }
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
