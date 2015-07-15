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

package com.prelert.job.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.AnomalyCause;
import com.prelert.utils.json.AutoDetectParseException;

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
    public void testEquals_GivenAnomalyCausesThatDifferInTypicalValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setTypical(cause1.getTypical() + 1.0);

        assertFalse(cause1.equals(cause2));
        assertFalse(cause2.equals(cause1));
    }

    @Test
    public void testEquals_GivenAnomalyCausesThatDifferInActualValue()
    {
        AnomalyCause cause1 = createFullyPopulatedAnomalyCause();
        AnomalyCause cause2 = createFullyPopulatedAnomalyCause();
        cause2.setActual(cause1.getActual() + 1.0);

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
    public void testParseJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                + "\"fieldName\" : \"groundspeed\","
                + "\"probability\" : 6.04434E-49,"
                + "\"byFieldName\" : \"status\","
                + "\"byFieldValue\" : \"Climb\","
                + "\"partitionFieldName\" : \"aircrafttype\","
                + "\"partitionFieldValue\" : \"A321\","
                + "\"function\" : \"mean\","
                + "\"typical\" : 442.616,"
                + "\"actual\" : 10.0,"
                + "\"influences\" : {"
                   + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                   + "\"user\": [{\"cat\": 1}]"
                + "},"
                + "\"overFieldName\" : \"callsign\","
                + "\"overFieldValue\" : \"HVN600\""
             + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        AnomalyCause cause = AnomalyCause.parseJson(parser);

        assertEquals(cause.getFieldName(), "groundspeed");
        assertEquals(cause.getProbability(), 6.04434E-49, 0.0001);
        assertEquals(cause.getByFieldName(), "status");
        assertEquals(cause.getByFieldValue(), "Climb");
        assertEquals(cause.getPartitionFieldName(), "aircrafttype");
        assertEquals(cause.getPartitionFieldValue(), "A321");
        assertEquals(cause.getFunction(), "mean");
        assertEquals(cause.getTypical(), 442.616, 0.001);
        assertEquals(cause.getActual(), 10.0, 0.0001);
        assertEquals(cause.getFunction(), "mean");
        assertEquals(cause.getOverFieldName(), "callsign");
        assertEquals(cause.getOverFieldValue(), "HVN600");

        List<Influence> influences = cause.getInfluences();

        Influence host = influences.get(0);
        assertEquals("host", host.getInfluenceField());
        assertEquals(2, host.getInfluenceScores().size());
        assertEquals("web-server", host.getInfluenceScores().get(0).getFieldValue());
        assertEquals(0.8, host.getInfluenceScores().get(0).getInfluence(), 0.001);
        assertEquals("localhost", host.getInfluenceScores().get(1).getFieldValue());
        assertEquals(0.7, host.getInfluenceScores().get(1).getInfluence(), 0.001);

        Influence user = influences.get(1);
        assertEquals("user", user.getInfluenceField());
        assertEquals(1, user.getInfluenceScores().size());
        assertEquals("cat", user.getInfluenceScores().get(0).getFieldValue());
        assertEquals(1.0, user.getInfluenceScores().get(0).getInfluence(), 0.001);
    }

    private static AnomalyCause createFullyPopulatedAnomalyCause()
    {
        AnomalyCause cause = new AnomalyCause();
        cause.setProbability(0.05);
        cause.setByFieldName("byName");
        cause.setByFieldValue("byValue");
        cause.setPartitionFieldName("partitionName");
        cause.setPartitionFieldValue("partitionValue");
        cause.setFunction("functionName");
        cause.setTypical(42.0);
        cause.setActual(100.0);
        cause.setFieldName("fieldName");
        cause.setOverFieldName("overName");
        cause.setOverFieldValue("overValue");
        return cause;
    }
}
