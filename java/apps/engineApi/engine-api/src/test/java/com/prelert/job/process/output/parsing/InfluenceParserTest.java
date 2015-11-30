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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.Influence;
import com.prelert.utils.json.AutoDetectParseException;

public class InfluenceParserTest
{
    private class InfluenceFieldComparator implements Comparator<Influence>
    {
        @Override
        public int compare(Influence o1, Influence o2)
        {
            return o1.getInfluencerFieldName().compareTo(o2.getInfluencerFieldName());
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
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("host", inf.getInfluencerFieldName());
        assertEquals(2, inf.getInfluencerFieldValues().size());
        assertEquals("web-server", inf.getInfluencerFieldValues().get(0));
        assertEquals("localhost", inf.getInfluencerFieldValues().get(1));
    }

    @Test
    public void testParseJson_noScores() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                            + "\"user\": []"
                     + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("user", inf.getInfluencerFieldName());
        assertEquals(0, inf.getInfluencerFieldValues().size());
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
        new InfluenceParser(parser).parseJson();
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
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(2, infs.size());
        Collections.sort(infs, new InfluenceFieldComparator());

        Influence host = infs.get(0);
        assertEquals("host", host.getInfluencerFieldName());
        assertEquals(2, host.getInfluencerFieldValues().size());
        assertEquals("web-server", host.getInfluencerFieldValues().get(0));
        assertEquals("localhost", host.getInfluencerFieldValues().get(1));

        Influence user = infs.get(1);
        assertEquals("user", user.getInfluencerFieldName());
        assertEquals(3, user.getInfluencerFieldValues().size());
        assertEquals("cat", user.getInfluencerFieldValues().get(0));
        assertEquals("dave", user.getInfluencerFieldValues().get(1));
        assertEquals("jo", user.getInfluencerFieldValues().get(2));
    }


    private static final JsonParser createJsonParser(String input) throws JsonParseException,
    IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
