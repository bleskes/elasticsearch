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

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.process.output.FlushAcknowledgement;
import com.prelert.utils.json.AutoDetectParseException;

public class FlushAcknowledgementParserTest
{
    @Test
    public void testParseJson()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{\"flush\": \"job-id\"}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        FlushAcknowledgement ack = new FlushAcknowledgementParser(parser).parseJson();

        assertEquals("job-id", ack.getId());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }


    private static final JsonParser createJsonParser(String input) throws JsonParseException,
    IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
