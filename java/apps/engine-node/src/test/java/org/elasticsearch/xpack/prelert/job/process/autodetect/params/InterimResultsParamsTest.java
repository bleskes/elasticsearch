
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import org.elasticsearch.test.ESTestCase;

public class InterimResultsParamsTest extends ESTestCase {
    public void testBuilder_GivenDefault() {
        InterimResultsParams params = InterimResultsParams.newBuilder().build();
        assertFalse(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    
    public void testBuilder_GivenCalcInterim() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    
    public void testBuilder_GivenCalcInterimAndStart() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(42L, null).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("42", params.getStart());
        assertEquals("", params.getEnd());
    }

    
    public void testBuilder_GivenCalcInterimAndEnd() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(null, 100L).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("100", params.getEnd());
    }

    
    public void testBuilder_GivenCalcInterimAndStartAndEnd() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(3600L, 7200L).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("3600", params.getStart());
        assertEquals("7200", params.getEnd());
    }

    
    public void testBuilder_GivenAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.newBuilder().advanceTime(1821L).build();
        assertFalse(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1821, params.getAdvanceTime());
    }

    
    public void testBuilder_GivenCalcInterimAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .advanceTime(1940L).build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1940, params.getAdvanceTime());
    }

    
    public void testBuilder_GivenCalcInterimWithTimeRangeAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(1L, 2L).advanceTime(1940L).build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("1", params.getStart());
        assertEquals("2", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1940, params.getAdvanceTime());
    }
}
