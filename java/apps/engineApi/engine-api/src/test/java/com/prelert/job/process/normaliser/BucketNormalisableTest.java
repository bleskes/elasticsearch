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

public class BucketNormalisableTest
{
    private static final double ERROR = 0.0001;
    private Bucket m_Bucket;
    private List<AnomalyRecord> m_Records;

    @Before
    public void setUp()
    {
        m_Bucket = new Bucket();
        m_Bucket.setAnomalyScore(42.0);
        m_Bucket.setRawAnomalyScore(4.2);
        m_Bucket.setMaxNormalizedProbability(2.0);
        AnomalyRecord record1 = new AnomalyRecord();
        record1.setNormalizedProbability(1.0);
        AnomalyRecord record2 = new AnomalyRecord();
        record2.setNormalizedProbability(2.0);
        m_Records = Arrays.asList(record1, record2);
        m_Bucket.setRecords(m_Records);
    }

    @Test
    public void testGetLevel()
    {
        assertEquals(Level.ROOT, new BucketNormalisable(m_Bucket).getLevel());
    }

    @Test
    public void testGetPartitionFieldName()
    {
        assertNull(new BucketNormalisable(m_Bucket).getPartitonFieldName());
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

    @Test
    public void testGetInitialScore()
    {
        assertEquals(4.2, new BucketNormalisable(m_Bucket).getInitialScore(), ERROR);
    }

    @Test
    public void testGetNormalisedScore()
    {
        assertEquals(42.0, new BucketNormalisable(m_Bucket).getNormalisedScore(), ERROR);
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

        assertEquals(2, children.size());
        assertEquals(1.0, children.get(0).getNormalisedScore(), ERROR);
        assertEquals(2.0, children.get(1).getNormalisedScore(), ERROR);
    }

    @Test
    public void testSetMaxChildrenScore()
    {
        new BucketNormalisable(m_Bucket).setMaxChildrenScore(42.0);

        assertEquals(42.0, m_Bucket.getMaxNormalizedProbability(), ERROR);
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
