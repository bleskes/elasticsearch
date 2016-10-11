
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import org.elasticsearch.test.ESTestCase;

public class TimeRangeTest extends ESTestCase {
    public void testGetStart() {
        assertEquals("", new TimeRange(null, null).getStart());
        assertEquals("10", new TimeRange(10L, null).getStart());
    }

    public void testGetEnd() {
        assertEquals("", new TimeRange(null, null).getEnd());
        assertEquals("20", new TimeRange(null, 20L).getEnd());
    }
}
