
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import org.elasticsearch.test.ESTestCase;

public class DataLoadParamsTest extends ESTestCase {
    public void testGetStart() {
        assertEquals("", new DataLoadParams(TimeRange.builder().build()).getStart());
        assertEquals("3", new DataLoadParams(TimeRange.builder().startTime("3").build()).getStart());
    }

    public void testGetEnd() {
        assertEquals("", new DataLoadParams(TimeRange.builder().build()).getEnd());
        assertEquals("1", new DataLoadParams(TimeRange.builder().endTime("1").build()).getEnd());
    }

    public void testIsResettingBuckets() {
        assertFalse(new DataLoadParams(TimeRange.builder().build()).isResettingBuckets());
        assertTrue(new DataLoadParams(TimeRange.builder().startTime("5").build()).isResettingBuckets());
    }
}
