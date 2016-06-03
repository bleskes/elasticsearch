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
import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.utils.json.AutoDetectParseException;

public class CategoryDefinitionParserTest
{
    @Test (expected = AutoDetectParseException.class)
    public void testParseJson_GivenCategoryDefinitionWithExamplesThatIsNotAnArrayObject()
            throws IOException
    {
        String input = "{\"categoryDefinition\": 1, \"examples\": \"bar\"}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        new CategoryDefinitionParser(parser).parseJson();
    }

    @Test
    public void testParseJson_GivenCategoryDefinitionWithAllFieldsPopulatedAndValid()
            throws IOException
    {
        String input = "{\"categoryDefinition\": 1,"
                     + " \"terms\":\"foo bar\","
                     + " \"regex\":\".*?foo.*?bar.*\","
                     + " \"maxMatchingLength\":350,"
                     + " \"examples\": [\"foo\", \"bar\"]}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        CategoryDefinition category = new CategoryDefinitionParser(parser).parseJson();

        assertEquals(1, category.getCategoryId());
        assertEquals("foo bar", category.getTerms());
        assertEquals(".*?foo.*?bar.*", category.getRegex());
        assertEquals(350L, category.getMaxMatchingLength());
        assertEquals(Arrays.asList("bar", "foo"), category.getExamples());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
