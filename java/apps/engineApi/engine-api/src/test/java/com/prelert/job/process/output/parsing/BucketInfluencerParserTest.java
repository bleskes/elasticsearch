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

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.BucketInfluencer;

public class BucketInfluencerParserTest
{
    private static final double ERROR = 0.0001;

    @Test
    public void testParseJson() throws IOException
    {
        String json = "{"
                + "\"probability\": 0.2,"
                + "\"initialAnomalyScore\": 10.0,"
                + "\"rawAnomalyScore\": 3.2,"
                + "\"influencerFieldName\": \"inf-name\""
         + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        BucketInfluencer influencer = new BucketInfluencerParser(parser).parseJson();

        assertEquals("inf-name", influencer.getInfluencerFieldName());
        assertEquals(0.2, influencer.getProbability(), ERROR);
        assertEquals(10.0, influencer.getInitialAnomalyScore(), ERROR);
        assertEquals(10.0, influencer.getAnomalyScore(), ERROR);
        assertEquals(3.2, influencer.getRawAnomalyScore(), ERROR);
    }

    @Test
    public void testParseJson_GivenUnexpectedField() throws IOException
    {
        String json = "{"
                + "\"unexpected\": 0.2"
         + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        BucketInfluencer influencer = new BucketInfluencerParser(parser).parseJson();
        assertEquals(new BucketInfluencer(), influencer);
    }

    private static final JsonParser createJsonParser(String input) throws IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
