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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import com.prelert.job.results.ModelDebugOutput;

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
    public void testEquals_GivenEqualModelDebugOutputs()
    {
        ModelDebugOutput modelDebugOutput1 = new ModelDebugOutput();
        modelDebugOutput1.setPartitionFieldName("part");
        modelDebugOutput1.setPartitionFieldValue("val");
        modelDebugOutput1.setDebugFeature("sum");
        modelDebugOutput1.setDebugLower(7.9);
        modelDebugOutput1.setDebugUpper(34.5);
        modelDebugOutput1.setDebugMean(12.7);
        modelDebugOutput1.setActual(100.0);

        ModelDebugOutput modelDebugOutput2 = new ModelDebugOutput();
        modelDebugOutput2.setPartitionFieldName("part");
        modelDebugOutput2.setPartitionFieldValue("val");
        modelDebugOutput2.setDebugFeature("sum");
        modelDebugOutput2.setDebugLower(7.9);
        modelDebugOutput2.setDebugUpper(34.5);
        modelDebugOutput2.setDebugMean(12.7);
        modelDebugOutput2.setActual(100.0);

        assertTrue(modelDebugOutput1.equals(modelDebugOutput2));
        assertTrue(modelDebugOutput2.equals(modelDebugOutput1));
    }
}
