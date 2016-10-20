
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import org.elasticsearch.test.ESTestCase;

public class DataLoadParamsTest extends ESTestCase {
    public void testGetStart() {
        assertEquals("", new DataLoadParams(new TimeRange(null, null)).getStart());
        assertEquals("3", new DataLoadParams(new TimeRange(3L, null)).getStart());
    }

    public void testGetEnd() {
        assertEquals("", new DataLoadParams(new TimeRange(null, null)).getEnd());
        assertEquals("1", new DataLoadParams(new TimeRange(null, 1L)).getEnd());
    }

    public void testIsResettingBuckets() {
        assertFalse(new DataLoadParams(new TimeRange(null, null)).isResettingBuckets());
        assertTrue(new DataLoadParams(new TimeRange(5L, null)).isResettingBuckets());
    }
}
