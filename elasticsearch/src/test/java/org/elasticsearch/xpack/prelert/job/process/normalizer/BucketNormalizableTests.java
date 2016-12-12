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
package org.elasticsearch.xpack.prelert.job.process.normalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.junit.Before;


public class BucketNormalizableTests extends ESTestCase {
    private static final double EPSILON = 0.0001;
    private Bucket bucket;

    @Before
    public void setUpBucket() {
        bucket = new Bucket("foo", new Date(), 600);

        BucketInfluencer bucketInfluencer1 = new BucketInfluencer("foo", bucket.getTimestamp(), 600, 1);
        bucketInfluencer1.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        bucketInfluencer1.setAnomalyScore(42.0);
        bucketInfluencer1.setProbability(0.01);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer("foo", bucket.getTimestamp(), 600, 2);
        bucketInfluencer2.setInfluencerFieldName("foo");
        bucketInfluencer2.setAnomalyScore(88.0);
        bucketInfluencer2.setProbability(0.001);

        bucket.setBucketInfluencers(Arrays.asList(bucketInfluencer1, bucketInfluencer2));

        bucket.setAnomalyScore(88.0);
        bucket.setMaxNormalizedProbability(2.0);

        AnomalyRecord record1 = new AnomalyRecord("foo", bucket.getTimestamp(), 600, 3);
        record1.setNormalizedProbability(1.0);
        AnomalyRecord record2 = new AnomalyRecord("foo", bucket.getTimestamp(), 600, 4);
        record2.setNormalizedProbability(2.0);
        bucket.setRecords(Arrays.asList(record1, record2));

        List<PartitionScore> partitionScores = new ArrayList<>();
        partitionScores.add(new PartitionScore("pf1", "pv1", 0.2, 0.1));
        partitionScores.add(new PartitionScore("pf1", "pv2", 0.4, 0.01));
        bucket.setPartitionScores(partitionScores);
    }

    public void testIsContainerOnly() {
        assertTrue(new BucketNormalizable(bucket).isContainerOnly());
    }

    public void testGetLevel() {
        assertEquals(Level.ROOT, new BucketNormalizable(bucket).getLevel());
    }

    public void testGetPartitionFieldName() {
        assertNull(new BucketNormalizable(bucket).getPartitionFieldName());
    }

    public void testGetPartitionFieldValue() {
        assertNull(new BucketNormalizable(bucket).getPartitionFieldValue());
    }

    public void testGetPersonFieldName() {
        assertNull(new BucketNormalizable(bucket).getPersonFieldName());
    }

    public void testGetFunctionName() {
        assertNull(new BucketNormalizable(bucket).getFunctionName());
    }

    public void testGetValueFieldName() {
        assertNull(new BucketNormalizable(bucket).getValueFieldName());
    }

    public void testGetProbability() {
        expectThrows(IllegalStateException.class, () -> new BucketNormalizable(bucket).getProbability());
    }

    public void testGetNormalizedScore() {
        assertEquals(88.0, new BucketNormalizable(bucket).getNormalizedScore(), EPSILON);
    }

    public void testSetNormalizedScore() {
        BucketNormalizable normalizable = new BucketNormalizable(bucket);

        normalizable.setNormalizedScore(99.0);

        assertEquals(99.0, normalizable.getNormalizedScore(), EPSILON);
        assertEquals(99.0, bucket.getAnomalyScore(), EPSILON);
    }

    public void testGetChildren() {
        List<Normalizable> children = new BucketNormalizable(bucket).getChildren();

        assertEquals(6, children.size());
        assertTrue(children.get(0) instanceof BucketInfluencerNormalizable);
        assertEquals(42.0, children.get(0).getNormalizedScore(), EPSILON);
        assertTrue(children.get(1) instanceof BucketInfluencerNormalizable);
        assertEquals(88.0, children.get(1).getNormalizedScore(), EPSILON);
        assertTrue(children.get(2) instanceof RecordNormalizable);
        assertEquals(1.0, children.get(2).getNormalizedScore(), EPSILON);
        assertTrue(children.get(3) instanceof RecordNormalizable);
        assertEquals(2.0, children.get(3).getNormalizedScore(), EPSILON);
        assertTrue(children.get(4) instanceof PartitionScoreNormalizable);
        assertEquals(0.2, children.get(4).getNormalizedScore(), EPSILON);
        assertTrue(children.get(5) instanceof PartitionScoreNormalizable);
        assertEquals(0.4, children.get(5).getNormalizedScore(), EPSILON);
    }

    public void testGetChildren_GivenTypeBucketInfluencer() {
        List<Normalizable> children = new BucketNormalizable(bucket).getChildren(0);

        assertEquals(2, children.size());
        assertTrue(children.get(0) instanceof BucketInfluencerNormalizable);
        assertEquals(42.0, children.get(0).getNormalizedScore(), EPSILON);
        assertTrue(children.get(1) instanceof BucketInfluencerNormalizable);
        assertEquals(88.0, children.get(1).getNormalizedScore(), EPSILON);
    }

    public void testGetChildren_GivenTypeRecord() {
        List<Normalizable> children = new BucketNormalizable(bucket).getChildren(1);

        assertEquals(2, children.size());
        assertTrue(children.get(0) instanceof RecordNormalizable);
        assertEquals(1.0, children.get(0).getNormalizedScore(), EPSILON);
        assertTrue(children.get(1) instanceof RecordNormalizable);
        assertEquals(2.0, children.get(1).getNormalizedScore(), EPSILON);
    }

    public void testGetChildren_GivenInvalidType() {
        expectThrows(IllegalArgumentException.class, () -> new BucketNormalizable(bucket).getChildren(3));
    }

    public void testSetMaxChildrenScore_GivenDifferentScores() {
        BucketNormalizable bucketNormalizable = new BucketNormalizable(bucket);

        assertTrue(bucketNormalizable.setMaxChildrenScore(0, 95.0));
        assertTrue(bucketNormalizable.setMaxChildrenScore(1, 42.0));

        assertEquals(95.0, bucket.getAnomalyScore(), EPSILON);
        assertEquals(42.0, bucket.getMaxNormalizedProbability(), EPSILON);
    }

    public void testSetMaxChildrenScore_GivenSameScores() {
        BucketNormalizable bucketNormalizable = new BucketNormalizable(bucket);

        assertFalse(bucketNormalizable.setMaxChildrenScore(0, 88.0));
        assertFalse(bucketNormalizable.setMaxChildrenScore(1, 2.0));

        assertEquals(88.0, bucket.getAnomalyScore(), EPSILON);
        assertEquals(2.0, bucket.getMaxNormalizedProbability(), EPSILON);
    }

    public void testSetMaxChildrenScore_GivenInvalidType() {
        expectThrows(IllegalArgumentException.class, () -> new BucketNormalizable(bucket).setMaxChildrenScore(3, 95.0));
    }

    public void testSetParentScore() {
        expectThrows(IllegalStateException.class, () -> new BucketNormalizable(bucket).setParentScore(42.0));
    }

    public void testResetBigChangeFlag() {
        BucketNormalizable normalizable = new BucketNormalizable(bucket);
        normalizable.raiseBigChangeFlag();

        normalizable.resetBigChangeFlag();

        assertFalse(bucket.hadBigNormalizedUpdate());
    }

    public void testRaiseBigChangeFlag() {
        BucketNormalizable normalizable = new BucketNormalizable(bucket);
        normalizable.resetBigChangeFlag();

        normalizable.raiseBigChangeFlag();

        assertTrue(bucket.hadBigNormalizedUpdate());
    }
}
