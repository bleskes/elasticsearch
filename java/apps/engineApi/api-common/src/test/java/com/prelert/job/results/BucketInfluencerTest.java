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

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class BucketInfluencerTest
{
    @Test
    public void testEquals_GivenNull()
    {
        assertFalse(new BucketInfluencer().equals(null));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        assertFalse(new BucketInfluencer().equals("a string"));
    }

    @Test
    public void testEquals_GivenEqualInfluencers()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setAnomalyScore(42.0);
        bucketInfluencer1.setInfluencerFieldName("foo");
        bucketInfluencer1.setInitialAnomalyScore(67.3);
        bucketInfluencer1.setProbability(0.0003);
        bucketInfluencer1.setRawAnomalyScore(3.14);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setAnomalyScore(42.0);
        bucketInfluencer2.setInfluencerFieldName("foo");
        bucketInfluencer2.setInitialAnomalyScore(67.3);
        bucketInfluencer2.setProbability(0.0003);
        bucketInfluencer2.setRawAnomalyScore(3.14);

        assertTrue(bucketInfluencer1.equals(bucketInfluencer2));
        assertTrue(bucketInfluencer2.equals(bucketInfluencer1));
        assertEquals(bucketInfluencer1.hashCode(), bucketInfluencer2.hashCode());
    }

    @Test
    public void testEquals_GivenDifferentAnomalyScore()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setAnomalyScore(42.0);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setAnomalyScore(42.1);

        assertFalse(bucketInfluencer1.equals(bucketInfluencer2));
        assertFalse(bucketInfluencer2.equals(bucketInfluencer1));
    }

    @Test
    public void testEquals_GivenDifferentFieldName()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setInfluencerFieldName("foo");

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setInfluencerFieldName("bar");

        assertFalse(bucketInfluencer1.equals(bucketInfluencer2));
        assertFalse(bucketInfluencer2.equals(bucketInfluencer1));
    }

    @Test
    public void testEquals_GivenDifferentInitialAnomalyScore()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setInitialAnomalyScore(42.0);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setInitialAnomalyScore(42.1);

        assertFalse(bucketInfluencer1.equals(bucketInfluencer2));
        assertFalse(bucketInfluencer2.equals(bucketInfluencer1));
    }

    @Test
    public void testEquals_GivenRawAnomalyScore()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setRawAnomalyScore(42.0);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setRawAnomalyScore(42.1);

        assertFalse(bucketInfluencer1.equals(bucketInfluencer2));
        assertFalse(bucketInfluencer2.equals(bucketInfluencer1));
    }

    @Test
    public void testEquals_GivenDifferentProbability()
    {
        BucketInfluencer bucketInfluencer1 = new BucketInfluencer();
        bucketInfluencer1.setProbability(0.001);

        BucketInfluencer bucketInfluencer2 = new BucketInfluencer();
        bucketInfluencer2.setProbability(0.002);

        assertFalse(bucketInfluencer1.equals(bucketInfluencer2));
        assertFalse(bucketInfluencer2.equals(bucketInfluencer1));
    }

    @Test
    public void testSerialise() throws IOException
    {
        BucketInfluencer bucketInfluencer = new BucketInfluencer();
        bucketInfluencer.setAnomalyScore(33.0);
        bucketInfluencer.setInfluencerFieldName("foo");
        bucketInfluencer.setInitialAnomalyScore(50.0);
        bucketInfluencer.setProbability(0.01);
        bucketInfluencer.setRawAnomalyScore(3.3);

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        bucketInfluencer.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"influencerFieldName\":\"foo\","
                + "\"anomalyScore\":33.0,"
                + "\"probability\":0.01,"
                + "\"rawAnomalyScore\":3.3,"
                + "\"initialAnomalyScore\":50.0"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
