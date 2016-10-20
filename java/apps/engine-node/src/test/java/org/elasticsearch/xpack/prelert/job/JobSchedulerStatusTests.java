package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.test.ESTestCase;

public class JobSchedulerStatusTests extends ESTestCase {

    public void testForString() {
        assertEquals(JobSchedulerStatus.fromString("started"), JobSchedulerStatus.STARTED);
        assertEquals(JobSchedulerStatus.fromString("stopping"), JobSchedulerStatus.STOPPING);
        assertEquals(JobSchedulerStatus.fromString("stopped"), JobSchedulerStatus.STOPPED);
    }

    public void testValidOrdinals() {
        assertEquals(0, JobSchedulerStatus.STARTED.ordinal());
        assertEquals(1, JobSchedulerStatus.STOPPING.ordinal());
        assertEquals(2, JobSchedulerStatus.STOPPED.ordinal());
    }

}
