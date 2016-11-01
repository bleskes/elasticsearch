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
package org.elasticsearch.xpack.prelert.modelsnapshots;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction;


public class PutModelSnapshotDescriptionTests extends ESTestCase {

    public void testUpdateDescription_GivenMissingArg() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new PutModelSnapshotDescriptionAction.Request(null, "foo", "bar"));
        assertEquals("[jobId] must not be null.", e.getMessage());

        e = expectThrows(IllegalArgumentException.class,
                () -> new PutModelSnapshotDescriptionAction.Request("foo", null, "bar"));
        assertEquals("[snapshotId] must not be null.", e.getMessage());

        e = expectThrows(IllegalArgumentException.class,
                () -> new PutModelSnapshotDescriptionAction.Request("foo", "foo", null));
        assertEquals("[description] must not be null.", e.getMessage());
    }


}
