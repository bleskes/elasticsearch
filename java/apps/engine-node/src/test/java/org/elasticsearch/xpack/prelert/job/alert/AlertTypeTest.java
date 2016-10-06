
package org.elasticsearch.xpack.prelert.job.alert;

import org.elasticsearch.test.ESTestCase;

import static org.junit.Assert.assertEquals;

public class AlertTypeTest extends ESTestCase {

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
