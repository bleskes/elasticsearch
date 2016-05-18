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

package com.prelert.job.quantiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class QuantilesTest
{
    @Test
    public void testEquals_GivenSameObject()
    {
        Quantiles quantiles = new Quantiles();
        assertTrue(quantiles.equals(quantiles));
    }

    @Test
    public void testEquals_GivenDifferentClassObject()
    {
        Quantiles quantiles = new Quantiles();
        assertFalse(quantiles.equals("not a quantiles object"));
    }

    @Test
    public void testEquals_GivenEqualQuantilesObject()
    {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("foo");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("foo");

        assertTrue(quantiles1.equals(quantiles2));
        assertTrue(quantiles2.equals(quantiles1));
    }

    @Test
    public void testEquals_GivenDifferentState()
    {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("bar1");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("bar2");

        assertFalse(quantiles1.equals(quantiles2));
        assertFalse(quantiles2.equals(quantiles1));
    }

    @Test
    public void testHashCode_GivenEqualObject()
    {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("foo");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("foo");

        assertEquals(quantiles1.hashCode(), quantiles2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
    {
        Quantiles quantiles = new Quantiles();
        quantiles.setTimestamp(new Date(1234L));
        quantiles.setQuantileState("foo");

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        quantiles.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"@timestamp\":1234,"
                + "\"quantileState\":\"foo\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
