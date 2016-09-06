/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.job.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class BucketTest
{
    @Test
    public void testEquals_GivenSameObject()
    {
        Bucket bucket = new Bucket();
        assertTrue(bucket.equals(bucket));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        Bucket bucket = new Bucket();
        assertFalse(bucket.equals("a string"));
    }

    @Test
    public void testEquals_GivenTwoDefaultBuckets()
    {
        Bucket bucket1 = new Bucket();
        Bucket bucket2 = new Bucket();

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentAnomalyScore()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(3.0);
        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(2.0);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenSameDates()
    {
        Bucket b1 = new Bucket();
        b1.setTimestamp(new Date(1234567890L));
        Bucket b2 = new Bucket();
        b2.setTimestamp(new Date(1234567890L));

    }

    @Test
    public void testEquals_GivenDifferentMaxNormalizedProbability()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setMaxNormalizedProbability(55.0);
        Bucket bucket2 = new Bucket();
        bucket2.setMaxNormalizedProbability(55.1);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentEventCount()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setEventCount(3);
        Bucket bucket2 = new Bucket();
        bucket2.setEventCount(100);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentRecordCount()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecordCount(300);
        Bucket bucket2 = new Bucket();
        bucket2.setRecordCount(400);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenOneHasRecordsAndTheOtherDoesNot()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(new AnomalyRecord()));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(null);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentNumberOfRecords()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(new AnomalyRecord()));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(Arrays.asList(new AnomalyRecord(), new AnomalyRecord()));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenSameNumberOfRecordsButDifferent()
    {
        AnomalyRecord anomalyRecord1 = new AnomalyRecord();
        anomalyRecord1.setAnomalyScore(1.0);
        AnomalyRecord anomalyRecord2 = new AnomalyRecord();
        anomalyRecord1.setAnomalyScore(2.0);

        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(anomalyRecord1));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(Arrays.asList(anomalyRecord2));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentIsInterim()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setInterim(true);
        Bucket bucket2 = new Bucket();
        bucket2.setInterim(false);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentInfluencers()
    {
        Bucket bucket1 = new Bucket();
        Influencer influencer = new Influencer("inf_field", "inf_value");
        Bucket bucket2 = new Bucket();
        bucket2.setInfluencers(Arrays.asList(influencer));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentBucketInfluencers()
    {
        Bucket bucket1 = new Bucket();
        BucketInfluencer influencer1 = new BucketInfluencer();
        influencer1.setInfluencerFieldName("foo");
        bucket1.addBucketInfluencer(influencer1);;

        Bucket bucket2 = new Bucket();
        BucketInfluencer influencer2 = new BucketInfluencer();
        influencer2.setInfluencerFieldName("bar");
        bucket2.addBucketInfluencer(influencer2);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenEqualBuckets()
    {
        AnomalyRecord record = new AnomalyRecord();
        Influencer influencer = new Influencer("testField", "testValue");
        BucketInfluencer bucketInfluencer = new BucketInfluencer();
        influencer.setProbability(0.1);
        influencer.setInitialAnomalyScore(10.0);
        Date date = new Date();

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(42.0);
        bucket1.setInitialAnomalyScore(92.0);
        bucket1.setEventCount(134);
        bucket1.setId("13546461");
        bucket1.setInterim(true);
        bucket1.setMaxNormalizedProbability(33.3);
        bucket1.setRecordCount(4);
        bucket1.setRecords(Arrays.asList(record));
        bucket1.addBucketInfluencer(bucketInfluencer);
        bucket1.setInfluencers(Arrays.asList(influencer));
        bucket1.setTimestamp(date);

        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(42.0);
        bucket2.setInitialAnomalyScore(92.0);
        bucket2.setEventCount(134);
        bucket2.setId("13546461");
        bucket2.setInterim(true);
        bucket2.setMaxNormalizedProbability(33.3);
        bucket2.setRecordCount(4);
        bucket2.setRecords(Arrays.asList(record));
        bucket2.addBucketInfluencer(bucketInfluencer);
        bucket2.setInfluencers(Arrays.asList(influencer));
        bucket2.setTimestamp(date);

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
        assertEquals(bucket1.hashCode(), bucket2.hashCode());
    }

    @Test
    public void testIsNormalisable_GivenNullBucketInfluencers()
    {
        Bucket bucket = new Bucket();
        bucket.setBucketInfluencers(null);
        bucket.setAnomalyScore(90.0);

        assertFalse(bucket.isNormalisable());
    }

    @Test
    public void testIsNormalisable_GivenEmptyBucketInfluencers()
    {
        Bucket bucket = new Bucket();
        bucket.setBucketInfluencers(Collections.emptyList());
        bucket.setAnomalyScore(90.0);

        assertFalse(bucket.isNormalisable());
    }

    @Test
    public void testIsNormalisable_GivenAnomalyScoreIsZeroAndRecordCountIsZero()
    {
        Bucket bucket = new Bucket();
        bucket.addBucketInfluencer(new BucketInfluencer());
        bucket.setAnomalyScore(0.0);
        bucket.setRecordCount(0);

        assertFalse(bucket.isNormalisable());
    }

    @Test
    public void testIsNormalisable_GivenAnomalyScoreIsZeroAndRecordCountIsNonZero()
    {
        Bucket bucket = new Bucket();
        bucket.addBucketInfluencer(new BucketInfluencer());
        bucket.setAnomalyScore(0.0);
        bucket.setRecordCount(1);

        assertTrue(bucket.isNormalisable());
    }

    @Test
    public void testIsNormalisable_GivenAnomalyScoreIsNonZeroAndRecordCountIsZero()
    {
        Bucket bucket = new Bucket();
        bucket.addBucketInfluencer(new BucketInfluencer());
        bucket.setAnomalyScore(1.0);
        bucket.setRecordCount(0);

        assertTrue(bucket.isNormalisable());
    }

    @Test
    public void testIsNormalisable_GivenAnomalyScoreIsNonZeroAndRecordCountIsNonZero()
    {
        Bucket bucket = new Bucket();
        bucket.addBucketInfluencer(new BucketInfluencer());
        bucket.setAnomalyScore(1.0);
        bucket.setRecordCount(1);

        assertTrue(bucket.isNormalisable());
    }

    @Test
    public void testSerialise_GivenBucketWithInfluencers() throws IOException
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setAnomalyScore(88.3);
        bucketInfluencer1.setInfluencerFieldName("foo");
        bucketInfluencer1.setInitialAnomalyScore(90.1);
        bucketInfluencer1.setProbability(0.003);
        bucketInfluencer1.setRawAnomalyScore(0.68);
        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setAnomalyScore(78.3);
        bucketInfluencer2.setInfluencerFieldName("bar");
        bucketInfluencer2.setInitialAnomalyScore(80.1);
        bucketInfluencer2.setProbability(0.04);
        bucketInfluencer2.setRawAnomalyScore(0.58);

        Bucket bucket = new Bucket();
        bucket.setAnomalyScore(88.3);
        bucket.setBucketSpan(600L);
        bucket.setRecordCount(142);
        bucket.setMaxNormalizedProbability(90.1);
        bucket.setEventCount(103);
        bucket.setBucketInfluencers(Arrays.asList(bucketInfluencer1, bucketInfluencer2));
        bucket.setInitialAnomalyScore(93.1);
        bucket.setTimestamp(new Date(1455753600000L));
        bucket.setProcessingTimeMs(20l);
        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();

        serialiser.startObject();
        bucket.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"anomalyScore\":88.3,"
                + "\"bucketSpan\":600,"
                + "\"@timestamp\":1455753600000,"
                + "\"processingTimeMs\":20,"
                + "\"recordCount\":142,"
                + "\"maxNormalizedProbability\":90.1,"
                + "\"eventCount\":103,"
                + "\"bucketInfluencers\":["
                +   "{"
                +      "\"influencerFieldName\":\"foo\","
                +      "\"anomalyScore\":88.3,"
                +      "\"probability\":0.003,"
                +      "\"rawAnomalyScore\":0.68,"
                +      "\"initialAnomalyScore\":90.1"
                +   "},"
                +   "{"
                +      "\"influencerFieldName\":\"bar\","
                +      "\"anomalyScore\":78.3,"
                +      "\"probability\":0.04,"
                +      "\"rawAnomalyScore\":0.58,"
                +      "\"initialAnomalyScore\":80.1"
                +   "}"
                + "],"
                + "\"initialAnomalyScore\":93.1"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    @Test
    public void testSerialise_GivenInterimBucketWithoutInfluencers() throws IOException
    {
        Bucket bucket = new Bucket();
        bucket.setAnomalyScore(88.3);
        bucket.setBucketSpan(600L);
        bucket.setRecordCount(142);
        bucket.setMaxNormalizedProbability(90.1);
        bucket.setEventCount(103);
        bucket.setBucketInfluencers(null);
        bucket.setInitialAnomalyScore(93.1);
        bucket.setTimestamp(new Date(1455753600000L));
        bucket.setInterim(true);
        bucket.setProcessingTimeMs(20l);
        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();

        serialiser.startObject();
        bucket.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"isInterim\":true,"
                + "\"anomalyScore\":88.3,"
                + "\"bucketSpan\":600,"
                + "\"@timestamp\":1455753600000,"
                + "\"processingTimeMs\":20,"
                + "\"recordCount\":142,"
                + "\"maxNormalizedProbability\":90.1,"
                + "\"eventCount\":103,"
                + "\"initialAnomalyScore\":93.1"
                + "}";

        assertEquals(expected, serialiser.toJson());
    }

    @Test
    public void testSetMaxNormalizedProbabilityPerPartition()
    {
        List<AnomalyRecord> records = new ArrayList<>();
        records.add(createAnomalyRecord("A", 20.0));
        records.add(createAnomalyRecord("A", 40.0));
        records.add(createAnomalyRecord("B", 90.0));
        records.add(createAnomalyRecord("B", 15.0));
        records.add(createAnomalyRecord("B", 45.0));

        Bucket bucket = new Bucket();
        bucket.setRecords(records);

        Map<String, Double> ppProb = bucket.calcMaxNormalizedProbabilityPerPartition();
        assertEquals(40.0, ppProb.get("A"), 0.0001);
        assertEquals(90.0, ppProb.get("B"), 0.0001);
    }

    private AnomalyRecord createAnomalyRecord(String partitionFieldValue,
                                        double normalizedProbability)
    {
        AnomalyRecord record = new AnomalyRecord();
        record.setPartitionFieldValue(partitionFieldValue);
        record.setNormalizedProbability(normalizedProbability);
        return record;
    }

    @Test
    public void testPartitionAnomalyScore()
    {
        List<PartitionScore> pScore = new ArrayList<>();
        pScore.add(new PartitionScore("pf", "pv1", 10, 0.1));
        pScore.add(new PartitionScore("pf", "pv3", 50, 0.1));
        pScore.add(new PartitionScore("pf", "pv4", 60, 0.1));
        pScore.add(new PartitionScore("pf", "pv2", 40, 0.1));

        Bucket bucket = new Bucket();
        bucket.setPartitionScores(pScore);

        double anomalyScore = bucket.partitionAnomalyScore("pv1");
        assertEquals(10.0, anomalyScore, 0.001);
        anomalyScore = bucket.partitionAnomalyScore("pv2");
        assertEquals(40.0, anomalyScore, 0.001);
        anomalyScore = bucket.partitionAnomalyScore("pv3");
        assertEquals(50.0, anomalyScore, 0.001);
        anomalyScore = bucket.partitionAnomalyScore("pv4");
        assertEquals(60.0, anomalyScore, 0.001);
    }
}
