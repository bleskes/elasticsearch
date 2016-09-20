
package org.elasticsearch.xpack.prelert.job;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobStatusTest {
    @Test
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
