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

public class AnomalyCauseTest
{
    @Test
    public void testHashCode_GivenEqualAnomalyCauses()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();

        assertEquals(cause1.hashCode(), cause2.hashCode());
    }

    @Test
    public void testEquals_GivenEqualSameCause()
    {
        AnomalyCause cause = new AnomalyCause();

        assertTrue(cause.equals(cause));
    }

    @Test
    public void testEquals_GivenEqualAnomalyCauses()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();

        assertTrue(cause1.equals(cause2));
        assertTrue(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInProbability()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setProbability(cause1.getProbability() + 0.01);

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInByFieldName()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setByFieldName("otherByFieldName");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInByFieldValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setByFieldValue("otherByFieldValue");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInCorrelatedByFieldValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setCorrelatedByFieldValue("otherCorrelatedByFieldValue");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInPartitionFieldName()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setPartitionFieldName("otherPartitionFieldName");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInPartitionFieldValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setPartitionFieldValue("otherPartitionFieldValue");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInFunctionName()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setFunction("otherFunctionName");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInFunctionDescription()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setFunctionDescription("otherFunctionDescription");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInTypicalValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setTypical(new double[] { cause1.getTypical()[0] + 1.0 });

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInActualValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setActual(new double[] { cause1.getActual()[0] + 1.0 });

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInFieldName()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setFieldName("otherFieldName");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInOverFieldName()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setOverFieldName("otherOverFieldName");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInOverFieldValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setOverFieldValue("otherOverFieldValue");

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testSerialise_GivenSingleActualAndTypical() throws IOException
    {
        AnomalyCause cause = createFullyPopulatedAnomalyCause();
        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();

        serialiser.startObject();
        cause.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"actual\":100.0,"
                + "\"overFieldValue\":\"overValue\","
                + "\"fieldName\":\"fieldName\","
                + "\"partitionFieldValue\":\"partitionValue\","
                + "\"overName.reversed\":\"overValue\","
                + "\"probability\":0.05,"
                + "\"byFieldValue\":\"byValue\","
                + "\"overFieldName\":\"overName\","
                + "\"partitionFieldName\":\"partitionName\","
                + "\"partitionName.reversed\":\"partitionValue\","
                + "\"byFieldName\":\"byName\","
                + "\"correlatedByFieldValue\":\"correlatedByValue\","
                + "\"function\":\"functionName\","
                + "\"typical\":42.0,"
                + "\"functionDescription\":\"functionDesc\","
                + "\"byName.reversed\":\"byValue\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    @Test
    public void testSerialise_GivenMultipleActualAndTypical() throws IOException
    {
        AnomalyCause cause = createFullyPopulatedAnomalyCause();
        cause.setActual(new double[] {1.0, 2.0});
        cause.setTypical(new double[] {3.0, 4.0});
        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();

        serialiser.startObject();
        cause.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"actual\":[1.0,2.0],"
                + "\"overFieldValue\":\"overValue\","
                + "\"fieldName\":\"fieldName\","
                + "\"partitionFieldValue\":\"partitionValue\","
                + "\"overName.reversed\":\"overValue\","
                + "\"probability\":0.05,"
                + "\"byFieldValue\":\"byValue\","
                + "\"overFieldName\":\"overName\","
                + "\"partitionFieldName\":\"partitionName\","
                + "\"partitionName.reversed\":\"partitionValue\","
                + "\"byFieldName\":\"byName\","
                + "\"correlatedByFieldValue\":\"correlatedByValue\","
                + "\"function\":\"functionName\","
                + "\"typical\":[3.0,4.0],"
                + "\"functionDescription\":\"functionDesc\","
                + "\"byName.reversed\":\"byValue\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    private static AnomalyCause createFullyPopulatedAnomalyCause()
    {
        AnomalyCause cause = new AnomalyCause();
        cause.setProbability(0.05);
        cause.setByFieldName("byName");
        cause.setByFieldValue("byValue");
        cause.setCorrelatedByFieldValue("correlatedByValue");
        cause.setPartitionFieldName("partitionName");
        cause.setPartitionFieldValue("partitionValue");
        cause.setFunction("functionName");
        cause.setFunctionDescription("functionDesc");
        cause.setTypical(new double[] { 42.0 });
        cause.setActual(new double[] { 100.0 });
        cause.setFieldName("fieldName");
        cause.setOverFieldName("overName");
        cause.setOverFieldValue("overValue");
        return cause;
    }
}
