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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.test.ESTestCase;

public class JobStateTests extends ESTestCase {

    public void testFromString() {
        assertEquals(JobState.fromString("closing"), JobState.CLOSING);
        assertEquals(JobState.fromString("closed"), JobState.CLOSED);
        assertEquals(JobState.fromString("failed"), JobState.FAILED);
        assertEquals(JobState.fromString("opened"), JobState.OPENED);
        assertEquals(JobState.fromString("CLOSING"), JobState.CLOSING);
        assertEquals(JobState.fromString("CLOSED"), JobState.CLOSED);
        assertEquals(JobState.fromString("FAILED"), JobState.FAILED);
        assertEquals(JobState.fromString("OPENED"), JobState.OPENED);
    }

    public void testToString() {
        assertEquals("closing", JobState.CLOSING.toString());
        assertEquals("closed", JobState.CLOSED.toString());
        assertEquals("failed", JobState.FAILED.toString());
        assertEquals("opened", JobState.OPENED.toString());
    }

    public void testValidOrdinals() {
        assertEquals(0, JobState.CLOSING.ordinal());
        assertEquals(1, JobState.CLOSED.ordinal());
        assertEquals(2, JobState.OPENING.ordinal());
        assertEquals(3, JobState.OPENED.ordinal());
        assertEquals(4, JobState.FAILED.ordinal());
    }

    public void testIsAnyOf() {
        assertFalse(JobState.OPENED.isAnyOf());
        assertFalse(JobState.OPENING.isAnyOf(JobState.CLOSED, JobState.FAILED));
        assertFalse(JobState.OPENED.isAnyOf(JobState.CLOSED, JobState.FAILED));
        assertFalse(JobState.CLOSED.isAnyOf(JobState.FAILED, JobState.OPENED));
        assertFalse(JobState.CLOSING.isAnyOf(JobState.FAILED, JobState.OPENED));

        assertTrue(JobState.OPENING.isAnyOf(JobState.OPENED, JobState.OPENING));
        assertTrue(JobState.OPENED.isAnyOf(JobState.OPENED, JobState.CLOSED));
        assertTrue(JobState.OPENED.isAnyOf(JobState.OPENED, JobState.CLOSED));
        assertTrue(JobState.CLOSED.isAnyOf(JobState.CLOSED));
        assertTrue(JobState.CLOSING.isAnyOf(JobState.CLOSING));
    }
}
