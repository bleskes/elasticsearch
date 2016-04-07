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

package com.prelert.job.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.DataCounts;
import com.prelert.job.persistence.JobDataCountsPersister;
import com.prelert.job.usage.UsageReporter;

public class StatusReporterTest
{
    private static final String JOB_ID = "SR";
    private static final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
    private static final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

    @Mock private UsageReporter m_UsageReporter;
    @Mock private JobDataCountsPersister m_JobDataCountsPersister;
    @Mock private Logger m_Logger;

    private StatusReporter m_StatusReporter;

    @BeforeClass
    public static void oneOffSetup()
    {
        System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
        System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));
    }

    @AfterClass
    public static void oneOffTeardown()
    {
        System.clearProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP);
        System.clearProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP);
    }

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_StatusReporter = new StatusReporter(JOB_ID, m_UsageReporter, m_JobDataCountsPersister,
                m_Logger);
    }

    @Test
    public void testSettingAcceptablePercentages()
    {
        assertEquals(m_StatusReporter.getAcceptablePercentDateParseErrors(), MAX_PERCENT_DATE_PARSE_ERRORS);
        assertEquals(m_StatusReporter.getAcceptablePercentOutOfOrderErrors(), MAX_PERCENT_OUT_OF_ORDER_ERRORS);
    }

    @Test
    public void testSimpleConstructor()
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        DataCounts stats = m_StatusReporter.incrementalStats();
        assertNotNull(stats);

        testAllFieldsEqualZero(stats);
    }

    @Test
    public void testComplexConstructor()
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        DataCounts counts = new DataCounts();

        counts.setProcessedRecordCount(1);
        counts.setInputBytes(2);
        counts.setInvalidDateCount(3);
        counts.setMissingFieldCount(4);
        counts.setOutOfOrderTimeStampCount(5);
        counts.setFailedTransformCount(6);
        counts.setExcludedRecordCount(7);

        m_StatusReporter = new StatusReporter(JOB_ID, counts, m_UsageReporter,
                m_JobDataCountsPersister, m_Logger);
        DataCounts stats = m_StatusReporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);

        assertEquals(1, m_StatusReporter.getProcessedRecordCount());
        assertEquals(2, m_StatusReporter.getBytesRead());
        assertEquals(3, m_StatusReporter.getDateParseErrorsCount());
        assertEquals(4, m_StatusReporter.getMissingFieldErrorCount());
        assertEquals(5, m_StatusReporter.getOutOfOrderRecordCount());
        assertEquals(6, m_StatusReporter.getFailedTransformCount());
        assertEquals(7, m_StatusReporter.getExcludedRecordCount());
    }

    @Test
    public void testResetIncrementalCounts()
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        IntrospectionException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        DataCounts stats = m_StatusReporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);

        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        m_StatusReporter.reportRecordWritten(5, 1000);
        m_StatusReporter.reportFailedTransform();
        m_StatusReporter.reportExcludedRecord(5);
        assertEquals(2, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(1, m_StatusReporter.incrementalStats().getFailedTransformCount());
        assertEquals(1, m_StatusReporter.incrementalStats().getExcludedRecordCount());
        assertEquals(1000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        assertEquals(m_StatusReporter.incrementalStats(), m_StatusReporter.runningTotalStats());

        m_StatusReporter.startNewIncrementalCount();
        stats = m_StatusReporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);
    }

    @Test
    public void testReportLatestTimeIncrementalStats()
    {
        m_StatusReporter.startNewIncrementalCount();
        m_StatusReporter.reportLatestTimeIncrementalStats(5001L);
        assertEquals(5001L, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    }

    @Test
    public void testReportRecordsWritten()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        m_StatusReporter.reportRecordWritten(5, 2000);
        assertEquals(1, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(5, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(2000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        m_StatusReporter.reportRecordWritten(5, 3000);
        m_StatusReporter.reportMissingField();
        assertEquals(2, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(2, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(5, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(3000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        assertEquals(m_StatusReporter.incrementalStats(), m_StatusReporter.runningTotalStats());

        verify(m_JobDataCountsPersister, never()).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testReportRecordsWritten_Given100Records()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 100; i++)
        {
            m_StatusReporter.reportRecordWritten(5, i);
        }

        assertEquals(100, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(500, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(100, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(300, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(100, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(m_JobDataCountsPersister, times(1)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testReportRecordsWritten_Given1000Records()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 1000; i++)
        {
            m_StatusReporter.reportRecordWritten(5, i);
        }

        assertEquals(1000, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(5000, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1000, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3000, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(1000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(m_JobDataCountsPersister, times(10)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testReportRecordsWritten_Given2000Records()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 2000; i++)
        {
            m_StatusReporter.reportRecordWritten(5, i);
        }

        assertEquals(2000, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10000, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(2000, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(6000, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(2000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(m_JobDataCountsPersister, times(11)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testReportRecordsWritten_Given20000Records()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 20000; i++)
        {
            m_StatusReporter.reportRecordWritten(5, i);
        }

        assertEquals(20000, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(100000, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(20000, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(60000, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(20000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(m_JobDataCountsPersister, times(29)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testReportRecordsWritten_Given30000Records()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 30000; i++)
        {
            m_StatusReporter.reportRecordWritten(5, i);
        }

        assertEquals(30000, m_StatusReporter.incrementalStats().getInputRecordCount());
        assertEquals(150000, m_StatusReporter.incrementalStats().getInputFieldCount());
        assertEquals(30000, m_StatusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(90000, m_StatusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(30000, m_StatusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(m_JobDataCountsPersister, times(30)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    @Test
    public void testFinishReporting()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.setAnalysedFieldsPerRecord(3);

        DataCounts dc = new DataCounts();
        dc.setExcludedRecordCount(1l);
        dc.setInputFieldCount(12l);
        dc.setMissingFieldCount(1l);
        dc.setProcessedFieldCount(5l);
        dc.setProcessedRecordCount(2l);
        dc.setLatestRecordTimeStamp(new Date(3000));

        m_StatusReporter.reportRecordWritten(5, 2000);
        m_StatusReporter.reportRecordWritten(5, 3000);
        m_StatusReporter.reportMissingField();
        m_StatusReporter.reportExcludedRecord(2);
        m_StatusReporter.finishReporting();

        Mockito.verify(m_UsageReporter, Mockito.times(1)).reportUsage();
        Mockito.verify(m_JobDataCountsPersister, Mockito.times(1)).persistDataCounts(eq("SR"), eq(dc));

        assertEquals(dc, m_StatusReporter.incrementalStats());
    }

    private void testAllFieldsEqualZero(DataCounts stats)
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        for(PropertyDescriptor propertyDescriptor :
            Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors())
        {
            if (propertyDescriptor.getReadMethod().getName().equals("getLatestRecordTimeStamp"))
            {
                Date val = (Date)propertyDescriptor.getReadMethod().invoke(stats);
                assertEquals(val, null);
                continue;
            }

            assertEquals(new Long(0), propertyDescriptor.getReadMethod().invoke(stats));
        }
    }
}
