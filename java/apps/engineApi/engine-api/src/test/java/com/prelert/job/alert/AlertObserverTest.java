/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

public class AlertObserverTest
{
    private class ConcreteObserver extends AlertObserver
    {
        public ConcreteObserver(AlertTrigger[] triggers)
        {
            super(triggers, "foo");
        }

        @Override
        public void fire(Bucket bucket, AlertTrigger trigger)
        {
        }
    }

    @Test
    public void testEvaluate_GivenScoreIsEqualToThreshold()
    {
        AlertTrigger at = new AlertTrigger(80.0, 39.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(80.0, 40.0)));
    }

    @Test
    public void testEvaluate_GivenScoreIsGreaterThanThreshold()
    {
        AlertTrigger at = new AlertTrigger(80.0, 50.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(80.1, 40.0)));

        at = new AlertTrigger(80.0, null, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(80.1, 40.0)));
    }

    @Test
    public void testEvaluate_GivenProbabilityIsEqualToThreshold()
    {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(3.0, 60.0)));

        at = new AlertTrigger(null, 60.0, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(3.0, 60.0)));
    }

    @Test
    public void testEvaluate_GivenProbabilityIsGreaterThanThreshold()
    {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(30.0, 60.1)));

        at = new AlertTrigger(null, 60.0, AlertType.BUCKET);
        ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(30.0, 60.1)));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityGreaterThanThresholds()
    {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertTrue(ao.evaluate(makeBucket(90.0, 90.0)));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityBelowThresholds()
    {
        AlertTrigger at = new AlertTrigger(40.0, 60.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertFalse(ao.evaluate(makeBucket(30.0, 50.0)));
    }

    @Test
    public void testEvaluate_GivenScoreAndProbabilityAreNotSet()
    {
        AlertTrigger at = new AlertTrigger(null, null, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});
        assertFalse(ao.evaluate(makeBucket(30.0, 50.0)));
    }

    @Test
    public void testTriggeredAlerts_bucketAlert()
    {
        AlertTrigger at = new AlertTrigger(90.0, 95.0, AlertType.BUCKET);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});

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

    @Test
    public void testTriggeredAlerts_InfluencerAlert()
    {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.INFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});

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

    @Test
    public void testTriggeredAlerts_BucketInfluencerAlert()
    {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at});

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

    @Test
    public void testTriggeredAlerts_OnlyInterimTriggersGivenInterimBucket()
    {
        AlertTrigger at = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER);
        AlertTrigger atInterim = new AlertTrigger(null, 90.0, AlertType.BUCKETINFLUENCER, true);
        AlertTrigger at2 = new AlertTrigger(null, 90.0, AlertType.INFLUENCER);
        AlertObserver ao = new ConcreteObserver(new AlertTrigger [] {at, atInterim, at2});

        Bucket bucket = makeInterimBucket(91.0, 91.0);
        BucketInfluencer bf = new BucketInfluencer();
        bf.setAnomalyScore(95.0);
        bucket.setBucketInfluencers(Arrays.asList(bf));

        List<AlertTrigger> triggered = null;
        triggered = ao.triggeredAlerts(bucket);
        assertEquals(1, triggered.size());
        assertSame(atInterim, triggered.get(0));
    }

    private Bucket makeBucket(double normalisedProb, double anomalyScore)
    {
        Bucket b = new Bucket();
        b.setAnomalyScore(anomalyScore);
        b.setMaxNormalizedProbability(normalisedProb);
        return b;
    }

    private Bucket makeInterimBucket(double normalisedProb, double anomalyScore)
    {
        Bucket b = new Bucket();
        b.setInterim(true);
        b.setAnomalyScore(anomalyScore);
        b.setMaxNormalizedProbability(normalisedProb);
        return b;
    }

    private Bucket makeInfluencerBucket(double ... anomalyScores)
    {
        List<Influencer> infs = new ArrayList<>();
        for (double score : anomalyScores)
        {
            Influencer inf = new Influencer();
            inf.setAnomalyScore(score);
            infs.add(inf);
        }

        Bucket b = new Bucket();
        b.setInfluencers(infs);
        return b;
    }

    private Bucket makeBucketInfluencerBucket(double ... anomalyScores)
    {
        List<BucketInfluencer> infs = new ArrayList<>();
        for (double score : anomalyScores)
        {
            BucketInfluencer inf = new BucketInfluencer();
            inf.setAnomalyScore(score);
            infs.add(inf);
        }

        Bucket b = new Bucket();
        b.setBucketInfluencers(infs);
        return b;
    }
}
