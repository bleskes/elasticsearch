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
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class ModelDebugOutputTest
{
    @Test
    public void testEquals_GivenSameObject()
    {
        ModelDebugOutput modelDebugOutput = new ModelDebugOutput();

        assertTrue(modelDebugOutput.equals(modelDebugOutput));
    }

    @Test
    public void testEquals_GivenObjectOfDifferentClass()
    {
        ModelDebugOutput modelDebugOutput = new ModelDebugOutput();

        assertFalse(modelDebugOutput.equals("a string"));
    }

    @Test
    public void testEquals_GivenDifferentTimestamp()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setTimestamp(new Date(0L));

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentPartitionFieldName()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setPartitionFieldName("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentPartitionFieldValue()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setPartitionFieldValue("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentByFieldName()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setByFieldName("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentByFieldValue()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setByFieldValue("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentOverFieldName()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setOverFieldName("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentOverFieldValue()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setOverFieldValue("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentDebugFeature()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setDebugFeature("another");

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentDebugLower()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setDebugLower(-1.0);

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentDebugUpper()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setDebugUpper(-1.0);

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentDebugMean()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setDebugMedian(-1.0);

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenDifferentActual()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();
        modelDebugOutput2.setActual(-1.0);

        assertFalse(modelDebugOutput1.equals(modelDebugOutput2));
        assertFalse(modelDebugOutput2.equals(modelDebugOutput1));
    }

    @Test
    public void testEquals_GivenEqualModelDebugOutputs()
    {
        ModelDebugOutput modelDebugOutput1 = createFullyPopulated();
        ModelDebugOutput modelDebugOutput2 = createFullyPopulated();

        assertTrue(modelDebugOutput1.equals(modelDebugOutput2));
        assertTrue(modelDebugOutput2.equals(modelDebugOutput1));
        assertEquals(modelDebugOutput1.hashCode(), modelDebugOutput2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
    {
        ModelDebugOutput modelDebugOutput = createFullyPopulated();

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        modelDebugOutput.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"actual\":100.0,"
                + "\"@timestamp\":12345678,"
                + "\"partitionFieldValue\":\"part_val\","
                + "\"by.reversed\":\"by_val\","
                + "\"debugFeature\":\"sum\","
                + "\"part.reversed\":\"part_val\","
                + "\"byFieldName\":\"by\","
                + "\"byFieldValue\":\"by_val\","
                + "\"debugLower\":7.9,"
                + "\"debugMedian\":12.7,"
                + "\"debugUpper\":34.5,"
                + "\"partitionFieldName\":\"part\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    private ModelDebugOutput createFullyPopulated()
    {
        ModelDebugOutput modelDebugOutput = new ModelDebugOutput();
        modelDebugOutput.setByFieldName("by");
        modelDebugOutput.setByFieldValue("by_val");
        modelDebugOutput.setPartitionFieldName("part");
        modelDebugOutput.setPartitionFieldValue("part_val");
        modelDebugOutput.setDebugFeature("sum");
        modelDebugOutput.setDebugLower(7.9);
        modelDebugOutput.setDebugUpper(34.5);
        modelDebugOutput.setDebugMedian(12.7);
        modelDebugOutput.setActual(100.0);
        modelDebugOutput.setTimestamp(new Date(12345678L));
        return modelDebugOutput;
    }
}
