
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import static org.junit.Assert.*;

public class DataCountsTest extends ESTestCase {


    public void testCountsEquals_GivenEqualCounts() {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        DataCounts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

        assertTrue(counts1.equals(counts2));
        assertTrue(counts2.equals(counts1));
    }


    public void testCountsHashCode_GivenEqualCounts() {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        DataCounts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

        assertEquals(counts1.hashCode(), counts2.hashCode());
    }


    public void testCountsCopyConstructor() {
        DataCounts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        DataCounts counts2 = new DataCounts(counts1);

        assertEquals(counts1.hashCode(), counts2.hashCode());
    }


    public void testCountCreatedZero() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        DataCounts counts = new DataCounts();
        testAllFieldsEqualZero(counts);
    }


    public void testCountCopyCreatedFieldsNotZero()
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        DataCounts counts1 = createCounts(1, 200, 400, 3, 4, 5, 6, 7, 8, 9, 10);
        testAllFieldsGreaterThanZero(counts1);

        DataCounts counts2 = new DataCounts(counts1);
        testAllFieldsGreaterThanZero(counts2);
    }


    public void testIncrements() {
        DataCounts counts = new DataCounts();

        counts.incrementFailedTransformCount(5);
        assertEquals(5, counts.getFailedTransformCount());

        counts.incrementInputBytes(15);
        assertEquals(15, counts.getInputBytes());

        counts.incrementInvalidDateCount(20);
        assertEquals(20, counts.getInvalidDateCount());

        counts.incrementMissingFieldCount(25);
        assertEquals(25, counts.getMissingFieldCount());

        counts.incrementOutOfOrderTimeStampCount(30);
        assertEquals(30, counts.getOutOfOrderTimeStampCount());

        counts.incrementProcessedRecordCount(40);
        assertEquals(40, counts.getProcessedRecordCount());
    }


    public void testGetInputRecordCount() {
        DataCounts counts = new DataCounts();
        counts.incrementProcessedRecordCount(5);
        assertEquals(5, counts.getInputRecordCount());

        counts.incrementOutOfOrderTimeStampCount(2);
        assertEquals(7, counts.getInputRecordCount());

        counts.incrementInvalidDateCount(1);
        assertEquals(8, counts.getInputRecordCount());
    }


    public void testCalcProcessedFieldCount() {
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(10);
        counts.setMissingFieldCount(0);
        counts.calcProcessedFieldCount(3);

        assertEquals(30, counts.getProcessedFieldCount());

        counts.setMissingFieldCount(5);
        counts.calcProcessedFieldCount(3);
        assertEquals(25, counts.getProcessedFieldCount());
    }


    public void testEquals() {
        DataCounts counts1 = new DataCounts();
        counts1.setBucketCount(3L);
        counts1.setFailedTransformCount(15L);
        counts1.setInputBytes(2000L);
        counts1.setInputFieldCount(300);
        counts1.setInvalidDateCount(6L);
        counts1.setMissingFieldCount(65L);
        counts1.setOutOfOrderTimeStampCount(40);
        counts1.setProcessedFieldCount(5000);
        counts1.setProcessedRecordCount(10);
        counts1.setLatestRecordTimeStamp(new Date(1435000000l));

        DataCounts counts2 = new DataCounts(counts1);

        assertEquals(counts1, counts2);
        counts2.setInvalidDateCount(8000L);
        assertFalse(counts1.equals(counts2));
    }

    private void testAllFieldsEqualZero(DataCounts stats)
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (PropertyDescriptor propertyDescriptor :
                Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors()) {
            if (propertyDescriptor.getDisplayName().equals("analysedFieldsPerRecord")) {
                continue;
            }

            if (propertyDescriptor.getReadMethod().getName().equals("getLatestRecordTimeStamp")) {
                Date val = (Date) propertyDescriptor.getReadMethod().invoke(stats);
                assertEquals(val, null);
                continue;
            }

            assertEquals(new Long(0), propertyDescriptor.getReadMethod().invoke(stats));
        }
    }

    private void testAllFieldsGreaterThanZero(DataCounts stats)
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (PropertyDescriptor propertyDescriptor :
                Introspector.getBeanInfo(DataCounts.class, Object.class).getPropertyDescriptors()) {
            if (propertyDescriptor.getReadMethod().getName().equals("getLatestRecordTimeStamp")) {
                Date val = (Date) propertyDescriptor.getReadMethod().invoke(stats);
                assertTrue("LastestRecordTimeStamp is not > 0", val.getTime() > 0);
            } else {
                Long val = (Long) propertyDescriptor.getReadMethod().invoke(stats);
                assertTrue("Field " + propertyDescriptor.getReadMethod().toString() + " not > 0", val > 0);
            }
        }
    }

    private static DataCounts createCounts(long bucketCount,
                                           long processedRecordCount, long processedFieldCount,
                                           long inputBytes, long inputFieldCount,
                                           long invalidDateCount, long missingFieldCount,
                                           long outOfOrderTimeStampCount, long failedTransformCount,
                                           long excludedRecordCount, long latestRecordTime) {
        DataCounts counts = new DataCounts();
        counts.setBucketCount(bucketCount);
        counts.setProcessedRecordCount(processedRecordCount);
        counts.setProcessedFieldCount(processedFieldCount);
        counts.setInputBytes(inputBytes);
        counts.setInputFieldCount(inputFieldCount);
        counts.setInvalidDateCount(invalidDateCount);
        counts.setMissingFieldCount(missingFieldCount);
        counts.setOutOfOrderTimeStampCount(outOfOrderTimeStampCount);
        counts.setFailedTransformCount(failedTransformCount);
        counts.setExcludedRecordCount(excludedRecordCount);
        counts.setLatestRecordTimeStamp(new Date(latestRecordTime));
        return counts;
    }

}
