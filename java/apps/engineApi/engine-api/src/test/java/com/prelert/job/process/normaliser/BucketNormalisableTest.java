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

package com.prelert.job.process.normaliser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;

public class BucketNormalisableTest
{
    private static final double ERROR = 0.0001;
    private Bucket m_Bucket;
    private List<AnomalyRecord> m_Records;

    @Before
    public void setUp()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        bucketInfluencer1.setAnomalyScore(42.0);
        bucketInfluencer1.setProbability(0.01);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setInfluencerFieldName("foo");
        bucketInfluencer2.setAnomalyScore(88.0);
        bucketInfluencer2.setProbability(0.001);

        m_Bucket = new Bucket();
        m_Bucket.setBucketInfluencers(Arrays.asList(bucketInfluencer1, bucketInfluencer2));

        m_Bucket.setAnomalyScore(88.0);
        m_Bucket.setMaxNormalizedProbability(2.0);
        AnomalyRecord record1 = new AnomalyRecord();
        record1.setNormalizedProbability(1.0);
        AnomalyRecord record2 = new AnomalyRecord();
        record2.setNormalizedProbability(2.0);
        m_Records = Arrays.asList(record1, record2);
        m_Bucket.setRecords(m_Records);
    }

    @Test
    public void testIsContainerOnly()
    {
        assertTrue(new BucketNormalisable(m_Bucket).isContainerOnly());
    }

    @Test
    public void testGetLevel()
    {
        assertEquals(Level.ROOT, new BucketNormalisable(m_Bucket).getLevel());
    }

    @Test
    public void testGetPartitionFieldName()
    {
        assertNull(new BucketNormalisable(m_Bucket).getPartitionFieldName());
    }

    @Test
    public void testGetPartitionFieldValue()
    {
        assertNull(new BucketNormalisable(m_Bucket).getPartitionFieldValue());
    }

    @Test
    public void testGetPersonFieldName()
    {
        assertNull(new BucketNormalisable(m_Bucket).getPersonFieldName());
    }

    @Test
    public void testGetFunctionName()
    {
        assertNull(new BucketNormalisable(m_Bucket).getFunctionName());
    }

    @Test
    public void testGetValueFieldName()
    {
        assertNull(new BucketNormalisable(m_Bucket).getValueFieldName());
    }

    @Test (expected = IllegalStateException.class)
    public void testGetProbability()
    {
        new BucketNormalisable(m_Bucket).getProbability();
    }

    @Test
    public void testGetNormalisedScore()
    {
        assertEquals(88.0, new BucketNormalisable(m_Bucket).getNormalisedScore(), ERROR);
    }

    @Test
    public void testSetNormalisedScore()
    {
        BucketNormalisable normalisable = new BucketNormalisable(m_Bucket);

        normalisable.setNormalisedScore(99.0);

        assertEquals(99.0, normalisable.getNormalisedScore(), ERROR);
        assertEquals(99.0, m_Bucket.getAnomalyScore(), ERROR);
    }

    @Test
    public void testGetChildren()
    {
        List<Normalisable> children = new BucketNormalisable(m_Bucket).getChildren();

        assertEquals(4, children.size());
        assertTrue(children.get(0) instanceof BucketInfluencerNormalisable);
        assertEquals(42.0, children.get(0).getNormalisedScore(), ERROR);
        assertTrue(children.get(1) instanceof BucketInfluencerNormalisable);
        assertEquals(88.0, children.get(1).getNormalisedScore(), ERROR);
        assertTrue(children.get(2) instanceof RecordNormalisable);
        assertEquals(1.0, children.get(2).getNormalisedScore(), ERROR);
        assertTrue(children.get(3) instanceof RecordNormalisable);
        assertEquals(2.0, children.get(3).getNormalisedScore(), ERROR);
    }

    @Test
    public void testGetChildren_GivenTypeBucketInfluencer()
    {
        List<Normalisable> children = new BucketNormalisable(m_Bucket).getChildren(0);

        assertEquals(2, children.size());
        assertTrue(children.get(0) instanceof BucketInfluencerNormalisable);
        assertEquals(42.0, children.get(0).getNormalisedScore(), ERROR);
        assertTrue(children.get(1) instanceof BucketInfluencerNormalisable);
        assertEquals(88.0, children.get(1).getNormalisedScore(), ERROR);
    }

    @Test
    public void testGetChildren_GivenTypeRecord()
    {
        List<Normalisable> children = new BucketNormalisable(m_Bucket).getChildren(1);

        assertEquals(2, children.size());
        assertTrue(children.get(0) instanceof RecordNormalisable);
        assertEquals(1.0, children.get(0).getNormalisedScore(), ERROR);
        assertTrue(children.get(1) instanceof RecordNormalisable);
        assertEquals(2.0, children.get(1).getNormalisedScore(), ERROR);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetChildren_GivenInvalidType()
    {
        new BucketNormalisable(m_Bucket).getChildren(2);
    }

    @Test
    public void testSetMaxChildrenScore_GivenDifferentScores()
    {
        BucketNormalisable bucketNormalisable = new BucketNormalisable(m_Bucket);

        assertTrue(bucketNormalisable.setMaxChildrenScore(0, 95.0));
        assertTrue(bucketNormalisable.setMaxChildrenScore(1, 42.0));

        assertEquals(95.0, m_Bucket.getAnomalyScore(), ERROR);
        assertEquals(42.0, m_Bucket.getMaxNormalizedProbability(), ERROR);
    }

    @Test
    public void testSetMaxChildrenScore_GivenSameScores()
    {
        BucketNormalisable bucketNormalisable = new BucketNormalisable(m_Bucket);

        assertFalse(bucketNormalisable.setMaxChildrenScore(0, 88.0));
        assertFalse(bucketNormalisable.setMaxChildrenScore(1, 2.0));

        assertEquals(88.0, m_Bucket.getAnomalyScore(), ERROR);
        assertEquals(2.0, m_Bucket.getMaxNormalizedProbability(), ERROR);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaxChildrenScore_GivenInvalidType()
    {
        new BucketNormalisable(m_Bucket).setMaxChildrenScore(2, 95.0);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetParentScore()
    {
        new BucketNormalisable(m_Bucket).setParentScore(42.0);
    }

    @Test
    public void testResetBigChangeFlag()
    {
        BucketNormalisable normalisable = new BucketNormalisable(m_Bucket);
        normalisable.raiseBigChangeFlag();

        normalisable.resetBigChangeFlag();

        assertFalse(m_Bucket.hadBigNormalisedUpdate());
    }

    @Test
    public void testRaiseBigChangeFlag()
    {
        BucketNormalisable normalisable = new BucketNormalisable(m_Bucket);
        normalisable.resetBigChangeFlag();

        normalisable.raiseBigChangeFlag();

        assertTrue(m_Bucket.hadBigNormalisedUpdate());
    }
}
