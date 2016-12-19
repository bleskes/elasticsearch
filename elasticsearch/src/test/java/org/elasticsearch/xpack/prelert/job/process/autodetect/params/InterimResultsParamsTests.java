/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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


    public void testBuilder_GivenCalcInterim() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }


    public void testBuilder_GivenCalcInterimAndStart() {
        InterimResultsParams params = InterimResultsParams.builder()
                .calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("42").build())
                .build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("42", params.getStart());
        assertEquals("43", params.getEnd());
    }

    public void testBuilder_GivenCalcInterimAndEnd_throws() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> InterimResultsParams.builder()
                .calcInterim(true)
                .forTimeRange(TimeRange.builder().endTime("100").build())
                .build());

        assertEquals("Invalid flush parameters: 'start' has not been specified.", e.getMessage());
    }


    public void testBuilder_GivenCalcInterimAndStartAndEnd() {
        InterimResultsParams params = InterimResultsParams.builder()
                .calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("3600").endTime("7200").build())
                .build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("3600", params.getStart());
        assertEquals("7200", params.getEnd());
    }


    public void testBuilder_GivenAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder().advanceTime("1821").build();
        assertFalse(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1821, params.getAdvanceTime());
    }


    public void testBuilder_GivenCalcInterimAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder()
                .calcInterim(true)
                .advanceTime("1940")
                .build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1940, params.getAdvanceTime());
    }


    public void testBuilder_GivenCalcInterimWithTimeRangeAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder()
                .calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("1").endTime("2").build())
                .advanceTime("1940")
                .build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("1", params.getStart());
        assertEquals("2", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1940, params.getAdvanceTime());
    }

    public void testValidate_GivenOnlyStartSpecified() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> InterimResultsParams.builder().forTimeRange(TimeRange.builder().startTime("1").build()).build());

        assertEquals("Invalid flush parameters: unexpected 'start'.", e.getMessage());
    }

    public void testFlushUpload_GivenOnlyEndSpecified() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> InterimResultsParams.builder().forTimeRange(TimeRange.builder().endTime("1").build()).build());

        assertEquals("Invalid flush parameters: unexpected 'end'.", e.getMessage());
    }

    public void testFlushUpload_GivenInterimResultsAndOnlyEndSpecified() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> InterimResultsParams.builder().calcInterim(true).forTimeRange(TimeRange.builder().endTime("1").build()).build());

        assertEquals("Invalid flush parameters: 'start' has not been specified.", e.getMessage());
    }

    public void testFlushUpload_GivenInterimResultsAndStartAndEndSpecifiedAsEpochs() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("1428494400").endTime("1428498000").build()).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428498000", params.getEnd());
    }


    public void testFlushUpload_GivenInterimResultsAndSameStartAndEnd() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("1428494400").endTime("1428494400").build()).build();

        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    public void testFlushUpload_GivenInterimResultsAndOnlyStartSpecified() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("1428494400").build()).build();

        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("1428494400", params.getStart());
        assertEquals("1428494401", params.getEnd());
    }

    public void testFlushUpload_GivenValidAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder().advanceTime("2015-04-08T13:00:00.000Z").build();
        assertFalse(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(1428498000L, params.getAdvanceTime());
    }

    public void testFlushUpload_GivenCalcInterimAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true).advanceTime("3600").build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(3600L, params.getAdvanceTime());
    }

    public void testFlushUpload_GivenCalcInterimWithTimeRangeAndAdvanceTime() {
        InterimResultsParams params = InterimResultsParams.builder().calcInterim(true)
                .forTimeRange(TimeRange.builder().startTime("150").endTime("300").build())
                .advanceTime("200").build();
        assertTrue(params.shouldCalculateInterim());
        assertEquals("150", params.getStart());
        assertEquals("300", params.getEnd());
        assertTrue(params.shouldAdvanceTime());
        assertEquals(200L, params.getAdvanceTime());
    }
}
