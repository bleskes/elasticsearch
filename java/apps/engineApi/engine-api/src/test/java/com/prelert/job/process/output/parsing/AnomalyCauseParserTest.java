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
package com.prelert.job.process.output.parsing;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.Influence;

public class AnomalyCauseParserTest
{
    @Test
    public void testParseJson() throws IOException
    {
        String json = "{"
                + "\"fieldName\" : \"groundspeed\","
                + "\"probability\" : 6.04434E-49,"
                + "\"byFieldName\" : \"status\","
                + "\"byFieldValue\" : \"Climb\","
                + "\"correlatedByFieldValue\" : \"Crash\","
                + "\"partitionFieldName\" : \"aircrafttype\","
                + "\"partitionFieldValue\" : \"A321\","
                + "\"function\" : \"low_mean\","
                + "\"functionDescription\" : \"mean\","
                + "\"typical\" : [ 442.616 ],"
                + "\"actual\" : [ 10.0 ],"
                + "\"influencers\" : {"
                   + "\"host\": [\"web-server\", \"localhost\"],"
                   + "\"user\": [\"cat\"]"
                + "},"
                + "\"overFieldName\" : \"callsign\","
                + "\"overFieldValue\" : \"HVN600\""
             + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        AnomalyCause cause = new AnomalyCauseParser(parser).parseJson();

        assertEquals("groundspeed", cause.getFieldName());
        assertEquals(6.04434E-49, cause.getProbability(), 0.0001);
        assertEquals("status", cause.getByFieldName());
        assertEquals("Climb", cause.getByFieldValue());
        assertEquals("Crash", cause.getCorrelatedByFieldValue());
        assertEquals("aircrafttype", cause.getPartitionFieldName());
        assertEquals("A321", cause.getPartitionFieldValue());
        assertEquals("low_mean", cause.getFunction());
        assertEquals("mean", cause.getFunctionDescription());
        assertEquals(442.616, cause.getTypical()[0], 0.001);
        assertEquals(10.0, cause.getActual()[0], 0.0001);
        assertEquals("callsign", cause.getOverFieldName());
        assertEquals("HVN600", cause.getOverFieldValue());

        List<Influence> influences = cause.getInfluencers();

        Influence host = influences.get(0);
        assertEquals("host", host.getInfluencerFieldName());
        assertEquals(2, host.getInfluencerFieldValues().size());
        assertEquals("web-server", host.getInfluencerFieldValues().get(0));
        assertEquals("localhost", host.getInfluencerFieldValues().get(1));

        Influence user = influences.get(1);
        assertEquals("user", user.getInfluencerFieldName());
        assertEquals(1, user.getInfluencerFieldValues().size());
        assertEquals("cat", user.getInfluencerFieldValues().get(0));
    }
}
