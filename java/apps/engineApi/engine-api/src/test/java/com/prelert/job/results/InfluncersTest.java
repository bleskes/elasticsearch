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

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.utils.json.AutoDetectParseException;

public class InfluncersTest {

    @Test
    public void testParseJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{\"probability\":0.9,\"initialScore\":97.1948,\"influencerFieldName\":\"src_ip\",\"influencerFieldValue\":\"23.28.243.150\"},";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        Influencer inf = Influencers.parseJson(parser);

        assertEquals(0.9, inf.getProbability(), 0.0001);
        assertEquals(97.1948, inf.getInitialScore(), 0.0001);
        assertEquals("src_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.150", inf.getInfluencerFieldValue());


        json = "{\"probability\":0.4,\"initialScore\":12.1948,\"influencerFieldName\":\"dst_ip\",\"influencerFieldValue\":\"23.28.243.1\"}";

        parser = createJsonParser(json);
        parser.nextToken();
        inf = Influencers.parseJson(parser);
        assertEquals(0.4, inf.getProbability(), 0.0001);
        assertEquals(12.1948, inf.getInitialScore(), 0.0001);
        assertEquals("dst_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.1", inf.getInfluencerFieldValue());
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
    IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
