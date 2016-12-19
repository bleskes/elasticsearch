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
package org.elasticsearch.xpack.prelert.scheduler;

import org.elasticsearch.test.ESTestCase;

public class SchedulerStatusTests extends ESTestCase {

    public void testForString() {
        assertEquals(SchedulerStatus.fromString("started"), SchedulerStatus.STARTED);
        assertEquals(SchedulerStatus.fromString("stopped"), SchedulerStatus.STOPPED);
    }

    public void testValidOrdinals() {
        assertEquals(0, SchedulerStatus.STARTED.ordinal());
        assertEquals(1, SchedulerStatus.STOPPED.ordinal());
    }

}
