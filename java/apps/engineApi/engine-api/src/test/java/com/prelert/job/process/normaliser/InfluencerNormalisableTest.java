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

import com.prelert.job.results.Influencer;

public class InfluencerNormalisableTest
{
    private static final double ERROR = 0.0001;
    private Influencer m_Influencer;

    @Before
    public void setUp()
    {
        m_Influencer = new Influencer();
        m_Influencer.setAnomalyScore(1.0);
        m_Influencer.setInfluencerFieldName("airline");
        m_Influencer.setInfluencerFieldValue("AAL");
        m_Influencer.setInitialAnomalyScore(2.0);
        m_Influencer.setProbability(0.05);
    }

    @Test
    public void testIsContainerOnly()
    {
        assertFalse(new InfluencerNormalisable(m_Influencer).isContainerOnly());
    }

    @Test
    public void testGetLevel()
    {
        assertEquals(Level.INFLUENCER, new InfluencerNormalisable(m_Influencer).getLevel());
    }

    @Test
    public void testGetPartitionFieldName()
    {
        assertNull(new InfluencerNormalisable(m_Influencer).getPartitionFieldName());
    }

    @Test
    public void testGetPartitionFieldValue()
    {
        assertNull(new InfluencerNormalisable(m_Influencer).getPartitionFieldValue());
    }

    @Test
    public void testGetPersonFieldName()
    {
        assertEquals("airline", new InfluencerNormalisable(m_Influencer).getPersonFieldName());
    }

    @Test
    public void testGetFunctionName()
    {
        assertNull(new InfluencerNormalisable(m_Influencer).getFunctionName());
    }

    @Test
    public void testGetValueFieldName()
    {
        assertNull(new InfluencerNormalisable(m_Influencer).getValueFieldName());
    }

    @Test
    public void testGetProbability()
    {
        assertEquals(0.05, new InfluencerNormalisable(m_Influencer).getProbability(), ERROR);
    }

    @Test
    public void testGetNormalisedScore()
    {
        assertEquals(1.0, new InfluencerNormalisable(m_Influencer).getNormalisedScore(), ERROR);
    }

    @Test
    public void testSetNormalisedScore()
    {
        InfluencerNormalisable normalisable = new InfluencerNormalisable(m_Influencer);

        normalisable.setNormalisedScore(99.0);

        assertEquals(99.0, normalisable.getNormalisedScore(), ERROR);
        assertEquals(99.0, m_Influencer.getAnomalyScore(), ERROR);
    }

    @Test
    public void testGetChildrenTypes()
    {
        assertTrue(new InfluencerNormalisable(m_Influencer).getChildrenTypes().isEmpty());
    }

    @Test (expected = IllegalStateException.class)
    public void testGetChildren_ByType()
    {
        new InfluencerNormalisable(m_Influencer).getChildren(0);
    }

    @Test
    public void testGetChildren()
    {
        assertTrue(new InfluencerNormalisable(m_Influencer).getChildren().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetMaxChildrenScore()
    {
        new InfluencerNormalisable(m_Influencer).setMaxChildrenScore(0, 42.0);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetParentScore()
    {
        new InfluencerNormalisable(m_Influencer).setParentScore(42.0);
    }

    @Test
    public void testResetBigChangeFlag()
    {
        InfluencerNormalisable normalisable = new InfluencerNormalisable(m_Influencer);
        normalisable.raiseBigChangeFlag();

        normalisable.resetBigChangeFlag();

        assertFalse(m_Influencer.hadBigNormalisedUpdate());
    }

    @Test
    public void testRaiseBigChangeFlag()
    {
        InfluencerNormalisable normalisable = new InfluencerNormalisable(m_Influencer);
        normalisable.resetBigChangeFlag();

        normalisable.raiseBigChangeFlag();

        assertTrue(m_Influencer.hadBigNormalisedUpdate());
    }
}
