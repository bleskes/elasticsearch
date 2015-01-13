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

package com.prelert.utils.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class FieldNameParserTest
{
    private static final double ERROR = 0.001;

    @Mock private Logger m_Logger;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown()
    {
        Mockito.verifyNoMoreInteractions(m_Logger);
    }

    @Test
    public void testParse_GivenParserDoesNotPointToStartObject() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        TestData data = new TestData();

        try {
            new TestFieldNameParser(parser, m_Logger).parse(data);
            Assert.fail();
        }
        catch (AutoDetectParseException e)
        {
            verify(m_Logger).error(
                    "Cannot parse TestData. First token 'null', is not the start object token");
        }
    }

    @Test
    public void testParse_GivenInvalidInt() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 0.0,"
                + "\"testLong\" : 2,"
                + "\"testDouble\" : 3.3,"
                + "\"testBoolean\" : true,"
                + "\"testString\" : \"foo\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(0, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testInt : 0.0 as an int");
    }

    @Test
    public void testParse_GivenInvalidLong() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 1,"
                + "\"testLong\" : 2.2,"
                + "\"testDouble\" : 3.3,"
                + "\"testBoolean\" : false,"
                + "\"testString\" : \"foo\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(1, data.getInt());
        assertEquals(0, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertFalse(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testLong : 2.2 as a long");
    }

    @Test
    public void testParse_GivenInvalidDouble() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 1,"
                + "\"testLong\" : 2,"
                + "\"testDouble\" : \"invalid\","
                + "\"testBoolean\" : true,"
                + "\"testString\" : \"foo\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(0.0, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testDouble : invalid as a double");
    }

    @Test
    public void testParse_GivenInvalidBoolean() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 1,"
                + "\"testLong\" : 2,"
                + "\"testDouble\" : 3.3,"
                + "\"testBoolean\" : \"invalid\","
                + "\"testString\" : \"foo\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertNull(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testBoolean : invalid as a boolean");
    }

    @Test
    public void testParse_GivenInvalidString() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 1,"
                + "\"testLong\" : 2,"
                + "\"testDouble\" : 3.3,"
                + "\"testBoolean\" : true,"
                + "\"testString\" : 1"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertNull(data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testString : 1 as a string");
    }

    @Test
    public void testParse_GivenFullyPopulatedAndValidTestData() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testInt\" : 1,"
                + "\"testLong\" : 2,"
                + "\"testDouble\" : 3.3,"
                + "\"testBoolean\" : true,"
                + "\"testString\" : \"foo\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parse(data);

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    @Test
    public void testParseAfterStartObject_GivenParserPointsToStartObject() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();
        TestData data = new TestData();

        new TestFieldNameParser(parser, m_Logger).parseAfterStartObject(data);

        assertEquals(0, data.getInt());
        assertEquals(0, data.getLong());
        assertEquals(0.0, data.getDouble(), ERROR);
        assertNull(data.getBoolean());
        assertNull(data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).error("Start object parsed in TestData");
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }

    private static class TestData
    {
        private int m_Int;
        private long m_Long;
        private double m_Double;
        private Boolean m_Boolean;
        private String m_String;

        public int getInt()
        {
            return m_Int;
        }

        public void setInt(int value)
        {
            this.m_Int = value;
        }

        public long getLong()
        {
            return m_Long;
        }

        public void setLong(long value)
        {
            this.m_Long = value;
        }

        public double getDouble()
        {
            return m_Double;
        }

        public void setDouble(double value)
        {
            this.m_Double = value;
        }

        public Boolean getBoolean()
        {
            return m_Boolean;
        }

        public void setBoolean(Boolean value)
        {
            this.m_Boolean = value;
        }

        public String getString()
        {
            return m_String;
        }

        public void setString(String value)
        {
            this.m_String = value;
        }
    }

    private static class TestFieldNameParser extends FieldNameParser<TestData>
    {

        public TestFieldNameParser(JsonParser jsonParser, Logger logger)
        {
            super("TestData", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, TestData data)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch(fieldName)
            {
                case "testInt":
                    data.setInt(parseAsIntOrZero(token, fieldName));
                    break;
                case "testLong":
                    data.setLong(parseAsLongOrZero(token, fieldName));
                    break;
                case "testDouble":
                    data.setDouble(parseAsDoubleOrZero(token, fieldName));
                    break;
                case "testBoolean":
                    data.setBoolean(parseAsBooleanOrNull(token, fieldName));
                    break;
                case "testString":
                    data.setString(parseAsStringOrNull(token, fieldName));
                    break;
                default:
                    m_Logger.error("Invalid fieldName: " + fieldName);
            }
        }
    }
}
