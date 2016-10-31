package org.elasticsearch.xpack.prelert.modelsnapshots;

/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */


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
