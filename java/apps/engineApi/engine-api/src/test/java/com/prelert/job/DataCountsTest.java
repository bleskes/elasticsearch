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

package com.prelert.job;

import static org.junit.Assert.*;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class DataCountsTest
{

    @Test
    public void testCountsEquals_GivenEqualCounts()
    {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        DataCounts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertTrue(counts1.equals(counts2));
        assertTrue(counts2.equals(counts1));
    }

    @Test
    public void testCountsHashCode_GivenEqualCounts()
    {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        DataCounts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(counts1.hashCode(), counts2.hashCode());
    }

    @Test
    public void testCountsCopyConstructor()
    {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        DataCounts counts2 = new DataCounts(counts1);

        assertEquals(counts1.hashCode(), counts2.hashCode());
    }

    @Test
    public void testCountCreatedZero() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException
    {
        DataCounts counts = new DataCounts();
        testAllFieldsEqualZero(counts);
    }

    @Test
    public void testCountCopyCreatedFieldsNotZero()
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        testAllFieldsGreaterThanZero(counts1);

        DataCounts counts2 = new DataCounts(counts1);
        testAllFieldsGreaterThanZero(counts2);
    }

    private void testAllFieldsEqualZero(DataCounts stats)
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        for(PropertyDescriptor propertyDescriptor :
            Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors())
        {
            assertEquals(new Long(0), propertyDescriptor.getReadMethod().invoke(stats));
        }
    }

    private void testAllFieldsGreaterThanZero(DataCounts stats)
    throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        for(PropertyDescriptor propertyDescriptor :
            Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors())
        {
            Long val = (Long)propertyDescriptor.getReadMethod().invoke(stats);
            assertTrue("Field " + propertyDescriptor.getReadMethod().toString() + " not > 0", val > 0);
        }
    }

    private static DataCounts createCounts(long bucketCount,
            long processedRecordCount, long processedFieldCount,
            long inputBytes, long inputFieldCount, long inputRecordCount,
            long invalidDateCount, long missingFieldCount,
            long outOfOrderTimeStampCount, long failedTransformCount)
    {
        DataCounts counts = new DataCounts();
        counts.setBucketCount(bucketCount);
        counts.setProcessedRecordCount(processedRecordCount);
        counts.setProcessedFieldCount(processedFieldCount);
        counts.setInputBytes(inputBytes);
        counts.setInputFieldCount(inputFieldCount);
        counts.setInputRecordCount(inputRecordCount);
        counts.setInvalidDateCount(invalidDateCount);
        counts.setMissingFieldCount(missingFieldCount);
        counts.setOutOfOrderTimeStampCount(outOfOrderTimeStampCount);
        counts.setFailedTransformCount(failedTransformCount);
        return counts;
    }

}
