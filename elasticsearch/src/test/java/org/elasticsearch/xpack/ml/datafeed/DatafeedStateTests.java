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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.test.ESTestCase;

public class DatafeedStateTests extends ESTestCase {

    public void testFromString() {
        assertEquals(DatafeedState.fromString("started"), DatafeedState.STARTED);
        assertEquals(DatafeedState.fromString("stopped"), DatafeedState.STOPPED);
    }

    public void testToString() {
        assertEquals("started", DatafeedState.STARTED.toString());
        assertEquals("stopped", DatafeedState.STOPPED.toString());
    }

    public void testValidOrdinals() {
        assertEquals(0, DatafeedState.STARTED.ordinal());
        assertEquals(1, DatafeedState.STOPPED.ordinal());
    }
}
