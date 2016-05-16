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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class InfluenceTest
{
    @Test
    public void testEquals()
    {
        Influence inf1 = new Influence("inf_field");
        Influence inf2 = new Influence("inf_field");

        assertEquals(inf1, inf2);

        inf1.setInfluencerFieldValues(Arrays.asList("a", "b"));
        inf2.setInfluencerFieldValues(Arrays.asList("a", "b"));

        assertEquals(inf1, inf2);
    }

    @Test
    public void testHashCode()
    {
        Influence inf1 = new Influence("inf_field");
        Influence inf2 = new Influence("inf_field");

        assertEquals(inf1.hashCode(), inf2.hashCode());

        inf1.setInfluencerFieldValues(Arrays.asList("a", "b"));
        inf2.setInfluencerFieldValues(Arrays.asList("a", "b"));

        assertEquals(inf1.hashCode(), inf2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
    {
        Influence influence = new Influence("foo");
        influence.setInfluencerFieldValues(Arrays.asList("a", "b"));

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        influence.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"influencerFieldName\":\"foo\","
                + "\"influencerFieldValues\":[\"a\",\"b\"]"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
