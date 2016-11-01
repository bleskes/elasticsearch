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
