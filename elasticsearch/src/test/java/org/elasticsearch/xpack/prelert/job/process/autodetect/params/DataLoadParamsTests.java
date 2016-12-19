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
        assertEquals("1", new DataLoadParams(TimeRange.builder().endTime("1").build()).getEnd());
    }

    public void testIsResettingBuckets() {
        assertFalse(new DataLoadParams(TimeRange.builder().build()).isResettingBuckets());
        assertTrue(new DataLoadParams(TimeRange.builder().startTime("5").build()).isResettingBuckets());
    }
}
