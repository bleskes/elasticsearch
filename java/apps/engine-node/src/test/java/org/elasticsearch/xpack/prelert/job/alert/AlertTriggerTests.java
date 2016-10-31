
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
