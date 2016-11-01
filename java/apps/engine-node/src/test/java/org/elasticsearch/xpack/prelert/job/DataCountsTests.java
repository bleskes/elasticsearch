/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.Date;

import static org.hamcrest.Matchers.greaterThan;

public class DataCountsTests extends AbstractSerializingTestCase<DataCounts> {

    @Override
    protected DataCounts createTestInstance() {
        DataCounts dataCounts = new DataCounts();
        dataCounts.setBucketCount(randomPositiveLong());
        dataCounts.setExcludedRecordCount(randomPositiveLong());
        dataCounts.setFailedTransformCount(randomPositiveLong());
        dataCounts.setInputBytes(randomPositiveLong());
        dataCounts.setInputFieldCount(randomPositiveLong());
        dataCounts.setInvalidDateCount(randomPositiveLong());
        if (randomBoolean()) {
            dataCounts.setLatestRecordTimeStamp(new Date(randomIntBetween(0, Integer.MAX_VALUE)));
        }
        dataCounts.setMissingFieldCount(randomPositiveLong());
        dataCounts.setOutOfOrderTimeStampCount(randomPositiveLong());
        dataCounts.setProcessedFieldCount(randomPositiveLong());
        dataCounts.setProcessedRecordCount(randomPositiveLong());
        return dataCounts;
    }

    @Override
    protected Writeable.Reader<DataCounts> instanceReader() {
        return DataCounts::new;
    }

    @Override
    protected DataCounts parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return DataCounts.PARSER.apply(parser, () -> matcher);
    }

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

    public void testCountCreatedZero() throws Exception {
        DataCounts counts = new DataCounts();
        assertAllFieldsEqualZero(counts);
    }

    public void testCountCopyCreatedFieldsNotZero() throws Exception {
        DataCounts counts1 = createCounts(1, 200, 400, 3, 4, 5, 6, 7, 8, 9, 10);
        assertAllFieldsGreaterThanZero(counts1);

        DataCounts counts2 = new DataCounts(counts1);
        assertAllFieldsGreaterThanZero(counts2);
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
        counts1.setLatestRecordTimeStamp(new Date(1435000000L));

        DataCounts counts2 = new DataCounts(counts1);

        assertEquals(counts1, counts2);
        counts2.setInvalidDateCount(8000L);
        assertFalse(counts1.equals(counts2));
    }

    private void assertAllFieldsEqualZero(DataCounts stats) throws Exception {
        assertEquals(0L, stats.getBucketCount());
        assertEquals(0L, stats.getProcessedRecordCount());
        assertEquals(0L, stats.getProcessedFieldCount());
        assertEquals(0L, stats.getInputBytes());
        assertEquals(0L, stats.getInputFieldCount());
        assertEquals(0L, stats.getInputRecordCount());
        assertEquals(0L, stats.getInvalidDateCount());
        assertEquals(0L, stats.getMissingFieldCount());
        assertEquals(0L, stats.getOutOfOrderTimeStampCount());
        assertEquals(0L, stats.getFailedTransformCount());
        assertEquals(0L, stats.getExcludedRecordCount());
        assertEquals(null, stats.getLatestRecordTimeStamp());
    }

    private void assertAllFieldsGreaterThanZero(DataCounts stats) throws Exception {
        assertThat(stats.getBucketCount(), greaterThan(0L));
        assertThat(stats.getProcessedRecordCount(), greaterThan(0L));
        assertThat(stats.getProcessedFieldCount(), greaterThan(0L));
        assertThat(stats.getInputBytes(), greaterThan(0L));
        assertThat(stats.getInputFieldCount(), greaterThan(0L));
        assertThat(stats.getInputRecordCount(), greaterThan(0L));
        assertThat(stats.getInputRecordCount(), greaterThan(0L));
        assertThat(stats.getInvalidDateCount(), greaterThan(0L));
        assertThat(stats.getMissingFieldCount(), greaterThan(0L));
        assertThat(stats.getOutOfOrderTimeStampCount(), greaterThan(0L));
        assertThat(stats.getFailedTransformCount(), greaterThan(0L));
        assertThat(stats.getExcludedRecordCount(), greaterThan(0L));
        assertThat(stats.getLatestRecordTimeStamp().getTime(), greaterThan(0L));
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
