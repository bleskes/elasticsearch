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
package com.prelert.job.process.output.parsing;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.Influence;
import com.prelert.utils.json.AutoDetectParseException;

public class AnomalyCauseParserTest
{
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
