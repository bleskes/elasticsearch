
package org.elasticsearch.xpack.prelert.job.alert;


import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AlertTests extends ESTestCase {

    public void testGettersAndSetters() throws URISyntaxException {
        Alert alert = new Alert();

        String s = new String("Mysterons");
        alert.setJobId(s);
        assertEquals(s, alert.getJobId());

        Date dt = new Date();
        alert.setTimestamp(dt);
        assertEquals(dt, alert.getTimestamp());

        double d = 66.5398281;
        alert.setAnomalyScore(d);
        assertEquals(d, alert.getAnomalyScore(), 0.0001);

        d = 99.99999;
        alert.setMaxNormalizedProbability(d);
        assertEquals(d, alert.getMaxNormalizedProbability(), 0.0001);

        URI u = new URI("http://beware.mysterons.com/index?message=death");
        alert.setUri(u);
        assertEquals(u, alert.getUri());

        assert (!alert.isTimeout());
        alert.setTimeout(true);
        assert (alert.isTimeout());

        assert (!alert.isInterim());
        alert.setInterim(true);
        assert (alert.isInterim());

        Bucket b = new Bucket();
        s = "Captain Scarlet";
        b.setId(s);
        alert.setBucket(b);
        assertEquals(b, alert.getBucket());
        assertEquals(s, alert.getBucket().getId());

        List<AnomalyRecord> l = new ArrayList<>();
        AnomalyRecord r = new AnomalyRecord();
        s = "Ozimandius";
        r.setId(s);
        l.add(r);
        alert.setRecords(l);
        assertEquals(l, alert.getRecords());
        assertEquals(s, alert.getRecords().get(0).getId());

        assertEquals(AlertType.BUCKET, alert.getAlertType());
        alert.setAlertType(AlertType.BUCKETINFLUENCER);
        assertEquals(AlertType.BUCKETINFLUENCER, alert.getAlertType());
    }
}
