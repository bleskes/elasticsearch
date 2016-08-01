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

import org.junit.Before;
import org.junit.Test;

import com.prelert.job.results.BucketInfluencer;

public class BucketInfluencerNormalisableTest
{
    private static final double ERROR = 0.0001;
    private BucketInfluencer m_Influencer;

    @Before
    public void setUp()
    {
        m_Influencer = new BucketInfluencer();
        m_Influencer.setInfluencerFieldName("airline");
        m_Influencer.setProbability(0.05);
        m_Influencer.setRawAnomalyScore(3.14);
        m_Influencer.setInitialAnomalyScore(2.0);
        m_Influencer.setAnomalyScore(1.0);
    }

    @Test
    public void testIsContainerOnly()
    {
        assertFalse(new BucketInfluencerNormalisable(m_Influencer).isContainerOnly());
    }

    @Test
    public void testGetLevel()
    {
        assertEquals(Level.BUCKET_INFLUENCER, new BucketInfluencerNormalisable(m_Influencer).getLevel());

        BucketInfluencer timeInfluencer = new BucketInfluencer();
        timeInfluencer.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        assertEquals(Level.ROOT, new BucketInfluencerNormalisable(timeInfluencer).getLevel());
    }

    @Test
    public void testGetPartitionFieldName()
    {
        assertNull(new BucketInfluencerNormalisable(m_Influencer).getPartitionFieldName());
    }

    @Test
    public void testGetPersonFieldName()
    {
        assertEquals("airline", new BucketInfluencerNormalisable(m_Influencer).getPersonFieldName());
    }

    @Test
    public void testGetFunctionName()
    {
        assertNull(new BucketInfluencerNormalisable(m_Influencer).getFunctionName());
    }

    @Test
    public void testGetValueFieldName()
    {
        assertNull(new BucketInfluencerNormalisable(m_Influencer).getValueFieldName());
    }

    @Test
    public void testGetProbability()
    {
        assertEquals(0.05, new BucketInfluencerNormalisable(m_Influencer).getProbability(), ERROR);
    }

    @Test
    public void testGetNormalisedScore()
    {
        assertEquals(1.0, new BucketInfluencerNormalisable(m_Influencer).getNormalisedScore(), ERROR);
    }

    @Test
    public void testSetNormalisedScore()
    {
        BucketInfluencerNormalisable normalisable = new BucketInfluencerNormalisable(m_Influencer);

        normalisable.setNormalisedScore(99.0);

        assertEquals(99.0, normalisable.getNormalisedScore(), ERROR);
        assertEquals(99.0, m_Influencer.getAnomalyScore(), ERROR);
    }

    @Test
    public void testGetChildrenTypes()
    {
        assertTrue(new BucketInfluencerNormalisable(m_Influencer).getChildrenTypes().isEmpty());
    }

    @Test (expected = IllegalStateException.class)
    public void testGetChildren_ByType()
    {
        new BucketInfluencerNormalisable(m_Influencer).getChildren(0);
    }

    @Test
    public void testGetChildren()
    {
        assertTrue(new BucketInfluencerNormalisable(m_Influencer).getChildren().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetMaxChildrenScore()
    {
        new BucketInfluencerNormalisable(m_Influencer).setMaxChildrenScore(0, 42.0);
    }

    @Test
    public void testSetParentScore()
    {
        new BucketInfluencerNormalisable(m_Influencer).setParentScore(42.0);

        assertEquals("airline", m_Influencer.getInfluencerFieldName());
        assertEquals(1.0, m_Influencer.getAnomalyScore(), ERROR);
        assertEquals(3.14, m_Influencer.getRawAnomalyScore(), ERROR);
        assertEquals(2.0, m_Influencer.getInitialAnomalyScore(), ERROR);
        assertEquals(0.05, m_Influencer.getProbability(), ERROR);
    }

    @Test
    public void testResetBigChangeFlag()
    {
        new BucketInfluencerNormalisable(m_Influencer).resetBigChangeFlag();
    }

    @Test
    public void testRaiseBigChangeFlag()
    {
        new BucketInfluencerNormalisable(m_Influencer).raiseBigChangeFlag();
    }
}
