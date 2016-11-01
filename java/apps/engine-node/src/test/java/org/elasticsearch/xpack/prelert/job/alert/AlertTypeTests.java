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
package org.elasticsearch.xpack.prelert.job.alert;

import org.elasticsearch.test.ESTestCase;

public class AlertTypeTests extends ESTestCase {

    public void testAlertTypes() {
        assertEquals("bucket", AlertType.BUCKET.toString());
        assertEquals("bucketinfluencer", AlertType.BUCKETINFLUENCER.toString());
        assertEquals("influencer", AlertType.INFLUENCER.toString());

        assertEquals(AlertType.BUCKET, AlertType.fromString("bucket"));
        assertEquals(AlertType.BUCKETINFLUENCER, AlertType.fromString("bucketinfluencer"));
        assertEquals(AlertType.INFLUENCER, AlertType.fromString("influencer"));

        boolean exception = false;
        try {
            AlertType.fromString("Non-Existent alert type here");
            assert (false);
        } catch (IllegalArgumentException ex) {
            exception = true;
        }
        assert (exception);
    }
}
