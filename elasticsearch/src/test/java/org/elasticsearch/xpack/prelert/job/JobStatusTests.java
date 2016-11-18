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

import org.elasticsearch.test.ESTestCase;

public class JobStatusTests extends ESTestCase {

    public void testForString() {
        assertEquals(JobStatus.fromString("closed"), JobStatus.CLOSED);
        assertEquals(JobStatus.fromString("closing"), JobStatus.CLOSING);
        assertEquals(JobStatus.fromString("failed"), JobStatus.FAILED);
        assertEquals(JobStatus.fromString("paused"), JobStatus.PAUSED);
        assertEquals(JobStatus.fromString("pausing"), JobStatus.PAUSING);
        assertEquals(JobStatus.fromString("running"), JobStatus.RUNNING);
    }

    public void testValidOrdinals() {
        assertEquals(0, JobStatus.RUNNING.ordinal());
        assertEquals(1, JobStatus.CLOSING.ordinal());
        assertEquals(2, JobStatus.CLOSED.ordinal());
        assertEquals(3, JobStatus.FAILED.ordinal());
        assertEquals(4, JobStatus.PAUSING.ordinal());
        assertEquals(5, JobStatus.PAUSED.ordinal());
    }

    public void testIsAnyOf() {
        assertFalse(JobStatus.RUNNING.isAnyOf());
        assertFalse(JobStatus.RUNNING.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));
        assertFalse(JobStatus.CLOSED.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));

        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING));
        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSED));
        assertTrue(JobStatus.PAUSED.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
        assertTrue(JobStatus.PAUSING.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
    }
}
