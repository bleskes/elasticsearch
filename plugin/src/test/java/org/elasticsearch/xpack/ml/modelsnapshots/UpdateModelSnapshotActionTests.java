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
package org.elasticsearch.xpack.ml.modelsnapshots;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.action.UpdateModelSnapshotAction;


public class UpdateModelSnapshotActionTests extends ESTestCase {

    public void testUpdateDescription_GivenMissingArg() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new UpdateModelSnapshotAction.Request(null, "foo"));
        assertEquals("[job_id] must not be null.", e.getMessage());

        e = expectThrows(IllegalArgumentException.class,
                () -> new UpdateModelSnapshotAction.Request("foo", null));
        assertEquals("[snapshot_id] must not be null.", e.getMessage());
    }
}
