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
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertObserverTests extends ESTestCase {
    private class ConcreteObserver extends AlertObserver {
        public ConcreteObserver(AlertTrigger[] triggers) {
            super(triggers, "foo");
        }

        @Override
        public void fire(Bucket bucket, AlertTrigger trigger) {
        }
    }

    public void testEvaluate_GivenScoreIsEqualToThreshold() {
        AlertTrigger at = new AlertTrigger(80.0, 39.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(80.0, 40.0)));
    }

    public void testEvaluate_GivenScoreIsGreaterThanThreshold() {
        AlertTrigger at = new AlertTrigger(80.0, 50.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(80.1, 40.0)));

        at = new AlertTrigger(80.0, null, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(80.1, 40.0)));
    }

    public void testEvaluate_GivenProbabilityIsEqualToThreshold() {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(3.0, 60.0)));

        at = new AlertTrigger(null, 60.0, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(3.0, 60.0)));
    }

    public void testEvaluate_GivenProbabilityIsGreaterThanThreshold() {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(30.0, 60.1)));

        at = new AlertTrigger(null, 60.0, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(30.0, 60.1)));
    }

    public void testEvaluate_GivenScoreAndProbabilityGreaterThanThresholds() {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertTrue(ao.evaluate(makeBucket(90.0, 90.0)));
    }

    public void testEvaluate_GivenScoreAndProbabilityBelowThresholds() {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertFalse(ao.evaluate(makeBucket(30.0, 50.0)));
    }

    public void testEvaluate_GivenScoreAndProbabilityAreNotSet() {
        AlertTrigger at = new AlertTrigger(null, null, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });
        assertFalse(ao.evaluate(makeBucket(30.0, 50.0)));
    }

    public void testTriggeredAlerts_bucketAlert() {
        AlertTrigger at = new AlertTrigger(90.0, 95.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });

        List<AlertTrigger> triggered = null;
        triggered = ao.triggeredAlerts(makeBucket(91.0, 96.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.BUCKET, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));

        triggered = ao.triggeredAlerts(makeBucket(1.0, 6.0));
        assertEquals(0, triggered.size());

        triggered = ao.triggeredAlerts(makeBucket(91.0, 10.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.BUCKET, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));

        triggered = ao.triggeredAlerts(makeBucket(1.0, 99.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.BUCKET, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));
    }

    public void testTriggeredAlerts_InfluencerAlert() {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.INFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });

        List<AlertTrigger> triggered = null;
        triggered = ao.triggeredAlerts(makeInfluencerBucket(91.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.INFLUENCER, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));

        triggered = ao.triggeredAlerts(makeInfluencerBucket(1.0, 6.0));
        assertEquals(0, triggered.size());

        triggered = ao.triggeredAlerts(makeInfluencerBucket(89.0, 91.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.INFLUENCER, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));
    }

    public void testTriggeredAlerts_BucketInfluencerAlert() {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at });

        List<AlertTrigger> triggered = null;
        triggered = ao.triggeredAlerts(makeBucketInfluencerBucket(91.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.BUCKETINFLUENCER, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));

        triggered = ao.triggeredAlerts(makeBucketInfluencerBucket(1.0, 6.0));
        assertEquals(0, triggered.size());

        triggered = ao.triggeredAlerts(makeBucketInfluencerBucket(89.0, 91.0));
        assertEquals(1, triggered.size());
        assertEquals(AlertType.BUCKETINFLUENCER, triggered.get(0).getAlertType());
        assertSame(at, triggered.get(0));
    }

    public void testTriggeredAlerts_OnlyInterimTriggersGivenInterimBucket() {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER);
        AlertTrigger atInterim = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER, true);
        AlertTrigger at2 = new AlertTrigger(null, 90.0, AlertType.INFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger[] { at, atInterim, at2 });

        Bucket bucket = makeInterimBucket(91.0, 91.0);
        BucketInfluencer bf = new BucketInfluencer();
        bf.setAnomalyScore(95.0);
        bucket.setBucketInfluencers(Arrays.asList(bf));

        List<AlertTrigger> triggered = null;
        triggered = ao.triggeredAlerts(bucket);
        assertEquals(1, triggered.size());
        assertSame(atInterim, triggered.get(0));
    }

    private Bucket makeBucket(double normalisedProb, double anomalyScore) {
        Bucket b = new Bucket();
        b.setAnomalyScore(anomalyScore);
        b.setMaxNormalizedProbability(normalisedProb);
        return b;
    }

    private Bucket makeInterimBucket(double normalisedProb, double anomalyScore) {
        Bucket b = new Bucket();
        b.setInterim(true);
        b.setAnomalyScore(anomalyScore);
        b.setMaxNormalizedProbability(normalisedProb);
        return b;
    }

    private Bucket makeInfluencerBucket(double... anomalyScores) {
        List<Influencer> infs = new ArrayList<>();
        for (double score : anomalyScores) {
            Influencer inf = new Influencer("foo", "bar");
            inf.setAnomalyScore(score);
            infs.add(inf);
        }

        Bucket b = new Bucket();
        b.setInfluencers(infs);
        return b;
    }

    private Bucket makeBucketInfluencerBucket(double... anomalyScores) {
        List<BucketInfluencer> infs = new ArrayList<>();
        for (double score : anomalyScores) {
            BucketInfluencer inf = new BucketInfluencer();
            inf.setAnomalyScore(score);
            infs.add(inf);
        }

        Bucket b = new Bucket();
        b.setBucketInfluencers(infs);
        return b;
    }
}
