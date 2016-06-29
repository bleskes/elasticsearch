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
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.utils.json.AutoDetectParseException;

public class ModelDebugOutputParserTest
{
    @Test (expected = AutoDetectParseException.class)
    public void testParseJson_GivenInvalidJson() throws IOException
    {
        String input = "\"debugFeature\": \"sum\" }";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        new ModelDebugOutputParser(parser).parseJson();
    }

    @Test
    public void testParseJson_GivenModelDebugOutputWithAllFieldsPopulatedAndValid()
            throws IOException
    {
        String input = "{\"debugFeature\": \"sum\","
                     + " \"partitionFieldName\":\"pn\","
                     + " \"partitionFieldValue\":\"pv\","
                     + " \"overFieldName\":\"on\","
                     + " \"overFieldValue\":\"ov\","
                     + " \"byFieldName\":\"bn\","
                     + " \"byFieldValue\":\"bv\","
                     + " \"timestamp\":1234567890000,"
                     + " \"debugLower\":12.7,"
                     + " \"debugMedian\":17.9,"
                     + " \"debugUpper\":24.2,"
                     + " \"actual\": 100.0}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ModelDebugOutput modelDebugOutput = new ModelDebugOutputParser(parser).parseJson();

        assertEquals(new Date(1234567890000L), modelDebugOutput.getTimestamp());
        assertEquals("pn", modelDebugOutput.getPartitionFieldName());
        assertEquals("pv", modelDebugOutput.getPartitionFieldValue());
        assertEquals("on", modelDebugOutput.getOverFieldName());
        assertEquals("ov", modelDebugOutput.getOverFieldValue());
        assertEquals("bn", modelDebugOutput.getByFieldName());
        assertEquals("bv", modelDebugOutput.getByFieldValue());
        assertEquals("sum", modelDebugOutput.getDebugFeature());
        assertEquals(12.7, modelDebugOutput.getDebugLower(), 1e-10);
        assertEquals(17.9, modelDebugOutput.getDebugMedian(), 1e-10);
        assertEquals(24.2, modelDebugOutput.getDebugUpper(), 1e-10);
        assertEquals(100.0, modelDebugOutput.getActual(), 1e-10);

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
