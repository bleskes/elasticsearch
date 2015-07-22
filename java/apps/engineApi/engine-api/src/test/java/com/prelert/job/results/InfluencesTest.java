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
            return o1.getInfluenceFieldName().compareTo(o2.getInfluenceFieldName());
        }
    }

    @Test
    public void testParseJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"host\": [\"web-server\", \"localhost\"]"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("host", inf.getInfluenceFieldName());
        assertEquals(2, inf.getInfluenceFieldValues().size());
        assertEquals("web-server", inf.getInfluenceFieldValues().get(0));
        assertEquals("localhost", inf.getInfluenceFieldValues().get(1));
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

        assertEquals("user", inf.getInfluenceFieldName());
        assertEquals(0, inf.getInfluenceFieldValues().size());
    }

    @Test(expected = AutoDetectParseException.class)
    public void testParseScores_InvalidJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        // invalid json
        String json = "{"
                + "\"user\": {}"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        Influences.parseJson(parser);
    }

    @Test
    public void testParseJson_withTwoInfluencers() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"host\": [\"web-server\", \"localhost\"],"
                            + "\"user\": [\"cat\", \"dave\", \"jo\"]"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = Influences.parseJson(parser);

        assertEquals(2, infs.size());
        Collections.sort(infs, new InfluenceFieldComparator());

        Influence host = infs.get(0);
        assertEquals("host", host.getInfluenceFieldName());
        assertEquals(2, host.getInfluenceFieldValues().size());
        assertEquals("web-server", host.getInfluenceFieldValues().get(0));
        assertEquals("localhost", host.getInfluenceFieldValues().get(1));

        Influence user = infs.get(1);
        assertEquals("user", user.getInfluenceFieldName());
        assertEquals(3, user.getInfluenceFieldValues().size());
        assertEquals("cat", user.getInfluenceFieldValues().get(0));
        assertEquals("dave", user.getInfluenceFieldValues().get(1));
        assertEquals("jo", user.getInfluenceFieldValues().get(2));
    }


    private static final JsonParser createJsonParser(String input) throws JsonParseException,
    IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
