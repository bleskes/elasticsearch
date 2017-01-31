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
package org.elasticsearch.xpack.ml.job.process.autodetect.params;

import org.elasticsearch.test.ESTestCase;

import java.util.Optional;

public class DataLoadParamsTests extends ESTestCase {
    public void testGetStart() {
        assertEquals("", new DataLoadParams(TimeRange.builder().build(), Optional.empty()).getStart());
        assertEquals("3", new DataLoadParams(TimeRange.builder().startTime("3").build(), Optional.empty()).getStart());
    }

    public void testGetEnd() {
        assertEquals("", new DataLoadParams(TimeRange.builder().build(), Optional.empty()).getEnd());
        assertEquals("1", new DataLoadParams(TimeRange.builder().endTime("1").build(), Optional.empty()).getEnd());
    }

    public void testIsResettingBuckets() {
        assertFalse(new DataLoadParams(TimeRange.builder().build(), Optional.empty()).isResettingBuckets());
        assertTrue(new DataLoadParams(TimeRange.builder().startTime("5").build(), Optional.empty()).isResettingBuckets());
    }
}
