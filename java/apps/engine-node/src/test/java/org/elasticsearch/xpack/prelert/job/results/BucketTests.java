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
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BucketTests extends AbstractSerializingTestCase<Bucket> {

    @Override
    protected Bucket createTestInstance() {
        String jobId = "foo";
        Bucket bucket = new Bucket(jobId);

        if (randomBoolean()) {
            bucket.setAnomalyScore(randomDouble());
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<BucketInfluencer> bucketInfluencers = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                BucketInfluencer bucketInfluencer = new BucketInfluencer(jobId);
                bucketInfluencer.setAnomalyScore(randomDouble());
                bucketInfluencer.setInfluencerFieldName(randomAsciiOfLengthBetween(1, 20));
                bucketInfluencer.setInitialAnomalyScore(randomDouble());
                bucketInfluencer.setProbability(randomDouble());
                bucketInfluencer.setRawAnomalyScore(randomDouble());
                bucketInfluencers.add(bucketInfluencer);
            }
            bucket.setBucketInfluencers(bucketInfluencers);
        }
        if (randomBoolean()) {
            bucket.setBucketSpan(randomPositiveLong());
        }
        if (randomBoolean()) {
            bucket.setEventCount(randomPositiveLong());
        }
        if (randomBoolean()) {
            bucket.setId(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<Influencer> influencers = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Influencer influencer = new Influencer(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                        randomAsciiOfLengthBetween(1, 20));
                influencer.setAnomalyScore(randomDouble());
                influencer.setInitialAnomalyScore(randomDouble());
                influencer.setProbability(randomDouble());
                influencer.setId(randomAsciiOfLengthBetween(1, 20));
                influencer.setInterim(randomBoolean());
                influencer.setTimestamp(new Date(randomLong()));
                influencers.add(influencer);
            }
            bucket.setInfluencers(influencers);
        }
        if (randomBoolean()) {
            bucket.setInitialAnomalyScore(randomDouble());
        }
        if (randomBoolean()) {
            bucket.setInterim(randomBoolean());
        }
        if (randomBoolean()) {
            bucket.setMaxNormalizedProbability(randomDouble());
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<PartitionScore> partitionScores = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                partitionScores.add(new PartitionScore(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20), randomDouble(),
                        randomDouble()));
            }
            bucket.setPartitionScores(partitionScores);
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            Map<String, Double> perPartitionMaxProbability = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                perPartitionMaxProbability.put(randomAsciiOfLengthBetween(1, 20), randomDouble());
            }
            bucket.setPerPartitionMaxProbability(perPartitionMaxProbability);
        }
        if (randomBoolean()) {
            bucket.setProcessingTimeMs(randomLong());
        }
        if (randomBoolean()) {
            bucket.setRecordCount(randomInt());
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<AnomalyRecord> records = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                AnomalyRecord anomalyRecord = new AnomalyRecord(jobId);
                anomalyRecord.setAnomalyScore(randomDouble());
                anomalyRecord.setActual(Collections.singletonList(randomDouble()));
                anomalyRecord.setTypical(Collections.singletonList(randomDouble()));
                anomalyRecord.setProbability(randomDouble());
                anomalyRecord.setId(randomAsciiOfLengthBetween(1, 20));
                anomalyRecord.setInterim(randomBoolean());
                anomalyRecord.setTimestamp(new Date(randomLong()));
                records.add(anomalyRecord);
            }
            bucket.setRecords(records);
        }
        if (randomBoolean()) {
            bucket.setTimestamp(new Date(randomLong()));
        }
        return bucket;
    }

    @Override
    protected Reader<Bucket> instanceReader() {
        return Bucket::new;
    }

    @Override
    protected Bucket parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Bucket.PARSER.apply(parser, () -> matcher);
    }

    public void testEquals_GivenDifferentClass() {
        Bucket bucket = new Bucket("foo");
        assertFalse(bucket.equals("a string"));
    }

    public void testEquals_GivenTwoDefaultBuckets() {
        Bucket bucket1 = new Bucket("foo");
        Bucket bucket2 = new Bucket("foo");

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentAnomalyScore() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setAnomalyScore(3.0);
        Bucket bucket2 = new Bucket("foo");
        bucket2.setAnomalyScore(2.0);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenSameDates() {
        Bucket b1 = new Bucket("foo");
        b1.setTimestamp(new Date(1234567890L));
        Bucket b2 = new Bucket("foo");
        b2.setTimestamp(new Date(1234567890L));

    }

    public void testEquals_GivenDifferentMaxNormalizedProbability() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setMaxNormalizedProbability(55.0);
        Bucket bucket2 = new Bucket("foo");
        bucket2.setMaxNormalizedProbability(55.1);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentEventCount() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setEventCount(3);
        Bucket bucket2 = new Bucket("foo");
        bucket2.setEventCount(100);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentRecordCount() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setRecordCount(300);
        Bucket bucket2 = new Bucket("foo");
        bucket2.setRecordCount(400);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenOneHasRecordsAndTheOtherDoesNot() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setId("1");
        bucket1.setRecords(Arrays.asList(new AnomalyRecord("foo")));
        Bucket bucket2 = new Bucket("foo");
        bucket2.setRecords(null);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentNumberOfRecords() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setRecords(Arrays.asList(new AnomalyRecord("foo")));
        Bucket bucket2 = new Bucket("foo");
        bucket2.setRecords(Arrays.asList(new AnomalyRecord("foo"), new AnomalyRecord("foo")));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenSameNumberOfRecordsButDifferent() {
        AnomalyRecord anomalyRecord1 = new AnomalyRecord("foo");
        anomalyRecord1.setAnomalyScore(1.0);
        AnomalyRecord anomalyRecord2 = new AnomalyRecord("foo");
        anomalyRecord1.setAnomalyScore(2.0);

        Bucket bucket1 = new Bucket("foo");
        bucket1.setRecords(Arrays.asList(anomalyRecord1));
        Bucket bucket2 = new Bucket("foo");
        bucket2.setRecords(Arrays.asList(anomalyRecord2));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentIsInterim() {
        Bucket bucket1 = new Bucket("foo");
        bucket1.setInterim(true);
        Bucket bucket2 = new Bucket("foo");
        bucket2.setInterim(false);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentInfluencers() {
        Bucket bucket1 = new Bucket("foo");
        Influencer influencer = new Influencer("foo", "inf_field", "inf_value");
        Bucket bucket2 = new Bucket("foo");
        bucket2.setInfluencers(Arrays.asList(influencer));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenDifferentBucketInfluencers() {
        Bucket bucket1 = new Bucket("foo");
        BucketInfluencer influencer1 = new BucketInfluencer("foo");
        influencer1.setInfluencerFieldName("foo");
        bucket1.addBucketInfluencer(influencer1);
        ;

        Bucket bucket2 = new Bucket("foo");
        BucketInfluencer influencer2 = new BucketInfluencer("foo");
        influencer2.setInfluencerFieldName("bar");
        bucket2.addBucketInfluencer(influencer2);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    public void testEquals_GivenEqualBuckets() {
        AnomalyRecord record = new AnomalyRecord("jobId");
        Influencer influencer = new Influencer("jobId", "testField", "testValue");
        BucketInfluencer bucketInfluencer = new BucketInfluencer("foo");
        influencer.setProbability(0.1);
        influencer.setInitialAnomalyScore(10.0);
        Date date = new Date();

        Bucket bucket1 = new Bucket("foo");
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

        Bucket bucket2 = new Bucket("foo");
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

    public void testIsNormalisable_GivenNullBucketInfluencers() {
        Bucket bucket = new Bucket("foo");
        bucket.setBucketInfluencers(null);
        bucket.setAnomalyScore(90.0);

        assertFalse(bucket.isNormalisable());
    }

    public void testIsNormalisable_GivenEmptyBucketInfluencers() {
        Bucket bucket = new Bucket("foo");
        bucket.setBucketInfluencers(Collections.emptyList());
        bucket.setAnomalyScore(90.0);

        assertFalse(bucket.isNormalisable());
    }

    public void testIsNormalisable_GivenAnomalyScoreIsZeroAndRecordCountIsZero() {
        Bucket bucket = new Bucket("foo");
        bucket.addBucketInfluencer(new BucketInfluencer("foo"));
        bucket.setAnomalyScore(0.0);
        bucket.setRecordCount(0);

        assertFalse(bucket.isNormalisable());
    }

    public void testIsNormalisable_GivenAnomalyScoreIsZeroAndRecordCountIsNonZero() {
        Bucket bucket = new Bucket("foo");
        bucket.addBucketInfluencer(new BucketInfluencer("foo"));
        bucket.setAnomalyScore(0.0);
        bucket.setRecordCount(1);

        assertTrue(bucket.isNormalisable());
    }

    public void testIsNormalisable_GivenAnomalyScoreIsNonZeroAndRecordCountIsZero() {
        Bucket bucket = new Bucket("foo");
        bucket.addBucketInfluencer(new BucketInfluencer("foo"));
        bucket.setAnomalyScore(1.0);
        bucket.setRecordCount(0);

        assertTrue(bucket.isNormalisable());
    }

    public void testIsNormalisable_GivenAnomalyScoreIsNonZeroAndRecordCountIsNonZero() {
        Bucket bucket = new Bucket("foo");
        bucket.addBucketInfluencer(new BucketInfluencer("foo"));
        bucket.setAnomalyScore(1.0);
        bucket.setRecordCount(1);

        assertTrue(bucket.isNormalisable());
    }

    public void testSetMaxNormalizedProbabilityPerPartition() {
        List<AnomalyRecord> records = new ArrayList<>();
        records.add(createAnomalyRecord("A", 20.0));
        records.add(createAnomalyRecord("A", 40.0));
        records.add(createAnomalyRecord("B", 90.0));
        records.add(createAnomalyRecord("B", 15.0));
        records.add(createAnomalyRecord("B", 45.0));

        Bucket bucket = new Bucket("foo");
        bucket.setRecords(records);

        Map<String, Double> ppProb = bucket.calcMaxNormalizedProbabilityPerPartition();
        assertEquals(40.0, ppProb.get("A"), 0.0001);
        assertEquals(90.0, ppProb.get("B"), 0.0001);
    }

    private AnomalyRecord createAnomalyRecord(String partitionFieldValue, double normalizedProbability) {
        AnomalyRecord record = new AnomalyRecord("foo");
        record.setPartitionFieldValue(partitionFieldValue);
        record.setNormalizedProbability(normalizedProbability);
        return record;
    }

    public void testPartitionAnomalyScore() {
        List<PartitionScore> pScore = new ArrayList<>();
        pScore.add(new PartitionScore("pf", "pv1", 10, 0.1));
        pScore.add(new PartitionScore("pf", "pv3", 50, 0.1));
        pScore.add(new PartitionScore("pf", "pv4", 60, 0.1));
        pScore.add(new PartitionScore("pf", "pv2", 40, 0.1));

        Bucket bucket = new Bucket("foo");
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
