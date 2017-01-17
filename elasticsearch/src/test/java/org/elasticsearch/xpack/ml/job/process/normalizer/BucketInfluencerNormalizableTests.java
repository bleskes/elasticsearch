/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.normalizer;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.results.BucketInfluencer;
import org.junit.Before;

import java.util.Date;


public class BucketInfluencerNormalizableTests extends ESTestCase {
    private static final double EPSILON = 0.0001;
    private static final String INDEX_NAME = "foo-index";
    private BucketInfluencer bucketInfluencer;

    @Before
    public void setUpBucketInfluencer() {
        bucketInfluencer = new BucketInfluencer("foo", new Date(), 600, 1);
        bucketInfluencer.setInfluencerFieldName("airline");
        bucketInfluencer.setProbability(0.05);
        bucketInfluencer.setRawAnomalyScore(3.14);
        bucketInfluencer.setInitialAnomalyScore(2.0);
        bucketInfluencer.setAnomalyScore(1.0);
    }

    public void testIsContainerOnly() {
        assertFalse(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).isContainerOnly());
    }

    public void testGetLevel() {
        assertEquals(Level.BUCKET_INFLUENCER, new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getLevel());

        BucketInfluencer timeInfluencer = new BucketInfluencer("foo", new Date(), 600, 1);
        timeInfluencer.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        assertEquals(Level.ROOT, new BucketInfluencerNormalizable(timeInfluencer, INDEX_NAME).getLevel());
    }

    public void testGetPartitionFieldName() {
        assertNull(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getPartitionFieldName());
    }

    public void testGetPersonFieldName() {
        assertEquals("airline", new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getPersonFieldName());
    }

    public void testGetFunctionName() {
        assertNull(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getFunctionName());
    }

    public void testGetValueFieldName() {
        assertNull(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getValueFieldName());
    }

    public void testGetProbability() {
        assertEquals(0.05, new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getProbability(), EPSILON);
    }

    public void testGetNormalizedScore() {
        assertEquals(1.0, new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getNormalizedScore(), EPSILON);
    }

    public void testSetNormalizedScore() {
        BucketInfluencerNormalizable normalizable = new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME);

        normalizable.setNormalizedScore(99.0);

        assertEquals(99.0, normalizable.getNormalizedScore(), EPSILON);
        assertEquals(99.0, bucketInfluencer.getAnomalyScore(), EPSILON);
    }

    public void testGetChildrenTypes() {
        assertTrue(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getChildrenTypes().isEmpty());
    }

    public void testGetChildren_ByType() {
        expectThrows(IllegalStateException.class, () -> new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME)
                .getChildren(Normalizable.ChildType.BUCKET_INFLUENCER));
    }

    public void testGetChildren() {
        assertTrue(new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).getChildren().isEmpty());
    }

    public void testSetMaxChildrenScore() {
        expectThrows(IllegalStateException.class,
                () -> new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME)
                        .setMaxChildrenScore(Normalizable.ChildType.BUCKET_INFLUENCER, 42.0));
    }

    public void testSetParentScore() {
        new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME).setParentScore(42.0);

        assertEquals("airline", bucketInfluencer.getInfluencerFieldName());
        assertEquals(1.0, bucketInfluencer.getAnomalyScore(), EPSILON);
        assertEquals(3.14, bucketInfluencer.getRawAnomalyScore(), EPSILON);
        assertEquals(2.0, bucketInfluencer.getInitialAnomalyScore(), EPSILON);
        assertEquals(0.05, bucketInfluencer.getProbability(), EPSILON);
    }

    public void testResetBigChangeFlag() {
        BucketInfluencerNormalizable normalizable = new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME);
        normalizable.resetBigChangeFlag();
        assertFalse(normalizable.hadBigNormalizedUpdate());
    }

    public void testRaiseBigChangeFlag() {
        BucketInfluencerNormalizable normalizable = new BucketInfluencerNormalizable(bucketInfluencer, INDEX_NAME);
        normalizable.raiseBigChangeFlag();
        assertTrue(normalizable.hadBigNormalizedUpdate());
    }
}
