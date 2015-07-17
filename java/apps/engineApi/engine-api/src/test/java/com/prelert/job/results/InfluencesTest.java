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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.elasticsearch.common.inject.Exposed;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.utils.json.AutoDetectParseException;

public class InfluencesTest
{
    private class InfluenceFieldComparator implements Comparator<Influence>
    {
        @Override
        public int compare(Influence o1, Influence o2)
        {
            return o1.getInfluenceField().compareTo(o2.getInfluenceField());
        }
    }

    @Test
    public void testParseJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}]"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("host", inf.getInfluenceField());
        assertEquals(2, inf.getInfluenceScores().size());
        assertEquals("web-server", inf.getInfluenceScores().get(0).getFieldValue());
        assertEquals(0.8, inf.getInfluenceScores().get(0).getInfluence(), 0.001);
        assertEquals("localhost", inf.getInfluenceScores().get(1).getFieldValue());
        assertEquals(0.7, inf.getInfluenceScores().get(1).getInfluence(), 0.001);
    }

    @Test
    public void testParseJson_noScores() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"user\": []"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("user", inf.getInfluenceField());
        assertEquals(0, inf.getInfluenceScores().size());
    }

    @Test(expected = AutoDetectParseException.class)
    public void testParseScores_noArray() throws JsonParseException, IOException, AutoDetectParseException
    {
        // invalid json
        String json = "{"
                + "\"user\": {}"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);
    }

    @Test
    public void testParseScores_extraField() throws JsonParseException, IOException, AutoDetectParseException
    {
        // invalid json
        String json = "{"
                + "\"user\": [{\"bob\" : 1.0, \"wrong\" : 2.0}]"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();

        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("user", inf.getInfluenceField());
        assertEquals(1, inf.getInfluenceScores().size());

        assertEquals("bob", inf.getInfluenceScores().get(0).getFieldValue());
        assertEquals(1.0, inf.getInfluenceScores().get(0).getInfluence(), 0.001);
    }

    @Test
    public void testParseJson_withTwoInfluencers() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                            + "\"user\": [{\"cat\": 1}, {\"dave\": 0.4},{\"jo\": 0.1}]"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(2, infs.size());
        Collections.sort(infs, new InfluenceFieldComparator());

        Influence host = infs.get(0);
        assertEquals("host", host.getInfluenceField());
        assertEquals(2, host.getInfluenceScores().size());
        assertEquals("web-server", host.getInfluenceScores().get(0).getFieldValue());
        assertEquals(0.8, host.getInfluenceScores().get(0).getInfluence(), 0.001);
        assertEquals("localhost", host.getInfluenceScores().get(1).getFieldValue());
        assertEquals(0.7, host.getInfluenceScores().get(1).getInfluence(), 0.001);

        Influence user = infs.get(1);
        assertEquals("user", user.getInfluenceField());
        assertEquals(3, user.getInfluenceScores().size());
        assertEquals("cat", user.getInfluenceScores().get(0).getFieldValue());
        assertEquals(1.0, user.getInfluenceScores().get(0).getInfluence(), 0.001);
        assertEquals("dave", user.getInfluenceScores().get(1).getFieldValue());
        assertEquals(0.4, user.getInfluenceScores().get(1).getInfluence(), 0.001);
        assertEquals("jo", user.getInfluenceScores().get(2).getFieldValue());
        assertEquals(0.1, user.getInfluenceScores().get(2).getInfluence(), 0.001);
    }


    private static final JsonParser createJsonParser(String input) throws JsonParseException,
    IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
