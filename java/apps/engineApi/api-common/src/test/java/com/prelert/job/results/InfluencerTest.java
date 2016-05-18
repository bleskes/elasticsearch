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

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class InfluencerTest
{
    @Test
    public void testEquals()
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialAnomalyScore(2.0);
        inf.setAnomalyScore(55.0);

        Influencer inf2 = new Influencer("a", "f");
        inf2.setTimestamp(new Date(123));
        inf2.setProbability(0.1);
        inf2.setInitialAnomalyScore(2.0);
        inf2.setAnomalyScore(55.0);

        assertEquals(inf, inf2);

        inf.setTimestamp(new Date(321));
        assertFalse(inf.equals(inf2));
    }

    @Test
    public void testHash()
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialAnomalyScore(2.0);
        inf.setAnomalyScore(55.0);

        Influencer inf2 = new Influencer("a", "f");
        inf2.setTimestamp(new Date(123));
        inf2.setProbability(0.1);
        inf2.setInitialAnomalyScore(2.0);
        inf2.setAnomalyScore(55.0);

        assertEquals(inf.hashCode(), inf2.hashCode());

        inf.setTimestamp(new Date(321));
        assertFalse(inf.hashCode() == inf2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialAnomalyScore(2.0);
        inf.setAnomalyScore(55.0);

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        inf.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"influencerFieldName\":\"a\","
                + "\"anomalyScore\":55.0,"
                + "\"@timestamp\":123,"
                + "\"a.reversed\":\"f\","
                + "\"probability\":0.1,"
                + "\"influencerFieldValue\":\"f\","
                + "\"initialAnomalyScore\":2.0"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    @Test
    public void testSerialise_GivenInterim() throws IOException
    {
        Influencer inf = new Influencer();
        inf.setTimestamp(new Date(123));
        inf.setInfluencerFieldName("a");
        inf.setInfluencerFieldValue("f");
        inf.setProbability(0.1);
        inf.setInitialAnomalyScore(2.0);
        inf.setAnomalyScore(55.0);
        inf.setInterim(true);

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        inf.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"influencerFieldName\":\"a\","
                + "\"isInterim\":true,"
                + "\"anomalyScore\":55.0,"
                + "\"@timestamp\":123,"
                + "\"a.reversed\":\"f\","
                + "\"probability\":0.1,"
                + "\"influencerFieldValue\":\"f\","
                + "\"initialAnomalyScore\":2.0"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
