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

package com.prelert.job.status;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.prelert.job.DataCounts;
import com.prelert.job.usage.UsageReporter;

public class StatusReporterTest
{

    @Test
    public void testSettingAcceptablePercentages()
    {
        final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
        final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

        System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
        System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

        DummyStatusReporter reporter = new DummyStatusReporter(mock(UsageReporter.class));

        assertEquals(reporter.getAcceptablePercentDateParseErrors(), MAX_PERCENT_DATE_PARSE_ERRORS);
        assertEquals(reporter.getAcceptablePercentOutOfOrderErrors(), MAX_PERCENT_OUT_OF_ORDER_ERRORS);
    }

    @Test
    public void testSimpleConstructor()
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        DummyStatusReporter reporter = new DummyStatusReporter(mock(UsageReporter.class));

        DataCounts stats = reporter.incrementalStats();
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

        DummyStatusReporter reporter = new DummyStatusReporter(counts, mock(UsageReporter.class));

        DataCounts stats = reporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);

        assertEquals(1, reporter.getProcessedRecordCount());
        assertEquals(2, reporter.getBytesRead());
        assertEquals(3, reporter.getDateParseErrorsCount());
        assertEquals(4, reporter.getMissingFieldErrorCount());
        assertEquals(5, reporter.getOutOfOrderRecordCount());
        assertEquals(6, reporter.getFailedTransformCount());
    }

    @Test
    public void testResetIncrementalCounts()
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        IntrospectionException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        DummyStatusReporter reporter = new DummyStatusReporter(mock(UsageReporter.class));
        DataCounts stats = reporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);

        reporter.setAnalysedFieldsPerRecord(3);

        reporter.reportRecordWritten(5);
        reporter.reportFailedTransform();
        assertEquals(1, reporter.incrementalStats().getInputRecordCount());
        assertEquals(5, reporter.incrementalStats().getInputFieldCount());
        assertEquals(1, reporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, reporter.incrementalStats().getProcessedFieldCount());
        assertEquals(1, reporter.incrementalStats().getFailedTransformCount());

        assertEquals(reporter.incrementalStats(), reporter.runningTotalStats());

        reporter.startNewIncrementalCount();
        stats = reporter.incrementalStats();
        assertNotNull(stats);
        testAllFieldsEqualZero(stats);
    }

    @Test
    public void testReportRecordsWritten()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        DummyStatusReporter reporter = new DummyStatusReporter(mock(UsageReporter.class));
        reporter.setAnalysedFieldsPerRecord(3);

        reporter.reportRecordWritten(5);
        assertEquals(1, reporter.incrementalStats().getInputRecordCount());
        assertEquals(5, reporter.incrementalStats().getInputFieldCount());
        assertEquals(1, reporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, reporter.incrementalStats().getProcessedFieldCount());

        reporter.reportRecordWritten(5);
        reporter.reportMissingField();
        assertEquals(2, reporter.incrementalStats().getInputRecordCount());
        assertEquals(10, reporter.incrementalStats().getInputFieldCount());
        assertEquals(2, reporter.incrementalStats().getProcessedRecordCount());
        assertEquals(5, reporter.incrementalStats().getProcessedFieldCount());

        assertEquals(reporter.incrementalStats(), reporter.runningTotalStats());
    }

    private void testAllFieldsEqualZero(DataCounts stats)
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        for(PropertyDescriptor propertyDescriptor :
            Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors())
        {
            if (propertyDescriptor.getDisplayName().equals("analysedFieldsPerRecord"))
            {
                continue;
            }

            assertEquals(new Long(0), propertyDescriptor.getReadMethod().invoke(stats));
        }
    }

}
