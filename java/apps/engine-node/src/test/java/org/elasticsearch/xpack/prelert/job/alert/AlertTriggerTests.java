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

public class AlertTriggerTests extends ESTestCase {

    public void testCons() {
        AlertTrigger trigger = new AlertTrigger(0.001, 0.3, AlertType.BUCKETINFLUENCER);

        assertEquals(0.001, trigger.getNormalisedThreshold(), 0.00001);
        assertEquals(0.3, trigger.getAnomalyThreshold(), 0.00001);
        assertEquals(AlertType.BUCKETINFLUENCER, trigger.getAlertType());
    }
}
