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

public class JobStatusTests extends ESTestCase {

    public void testForString() {
        assertEquals(JobStatus.fromString("closed"), JobStatus.CLOSED);
        assertEquals(JobStatus.fromString("closing"), JobStatus.CLOSING);
        assertEquals(JobStatus.fromString("failed"), JobStatus.FAILED);
        assertEquals(JobStatus.fromString("opening"), JobStatus.OPENING);
        assertEquals(JobStatus.fromString("opened"), JobStatus.OPENED);
    }

    public void testValidOrdinals() {
        assertEquals(0, JobStatus.CLOSING.ordinal());
        assertEquals(1, JobStatus.CLOSED.ordinal());
        assertEquals(2, JobStatus.OPENING.ordinal());
        assertEquals(3, JobStatus.OPENED.ordinal());
        assertEquals(4, JobStatus.FAILED.ordinal());
    }

    public void testIsAnyOf() {
        assertFalse(JobStatus.OPENED.isAnyOf());
        assertFalse(JobStatus.OPENED.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.OPENING));
        assertFalse(JobStatus.CLOSED.isAnyOf(JobStatus.CLOSING, JobStatus.FAILED, JobStatus.OPENING, JobStatus.OPENED));

        assertTrue(JobStatus.OPENED.isAnyOf(JobStatus.OPENED));
        assertTrue(JobStatus.OPENED.isAnyOf(JobStatus.OPENED, JobStatus.CLOSED));
        assertTrue(JobStatus.CLOSING.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING));
        assertTrue(JobStatus.CLOSED.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING));
    }
}
