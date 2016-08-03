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

package com.prelert.utils.json;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

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
    public void testParseJson_GivenParserDoesNotPointToStartObject() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);

        try {
            new TestFieldNameParser(parser, m_Logger).parseJson();
            fail();
        }
        catch (AutoDetectParseException e)
        {
            verify(m_Logger).error(
                    "Cannot parse TestData. First token 'null', is not the start object token");
        }
    }

    @Test
    public void testParseJson_GivenInvalidInt() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(0, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testInt : 0.0 as an int");
    }

    @Test
    public void testParseJson_GivenInvalidLong() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(1, data.getInt());
        assertEquals(0, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertFalse(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testLong : 2.2 as a long");
    }

    @Test
    public void testParseJson_GivenInvalidDouble() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(0.0, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testDouble : invalid as a double");
    }

    @Test
    public void testParseJson_GivenInvalidBoolean() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertNull(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testBoolean : invalid as a boolean");
    }

    @Test
    public void testParseJson_GivenInvalidString() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertNull(data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).warn("Cannot parse testString : 1 as a string");
    }

    @Test
    public void testParseJson_GivenFullyPopulatedAndValidTestData() throws JsonParseException,
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

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertEquals(1, data.getInt());
        assertEquals(2, data.getLong());
        assertEquals(3.3, data.getDouble(), ERROR);
        assertTrue(data.getBoolean());
        assertEquals("foo", data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    @Test
    public void testParseJsonAfterStartObject_GivenParserPointsToStartObject() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJsonAfterStartObject();

        assertEquals(0, data.getInt());
        assertEquals(0, data.getLong());
        assertEquals(0.0, data.getDouble(), ERROR);
        assertNull(data.getBoolean());
        assertNull(data.getString());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());

        verify(m_Logger).error("Start object parsed in TestData");
    }

    @Test
    public void testParseStringArray() throws JsonParseException,
            IOException, AutoDetectParseException
    {
        String input = "{\"testStringArray\" : [\"boat\", \"yacht\", \"ship\"]}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        TestData data = new TestFieldNameParser(parser, m_Logger).parseJson();

        assertNotNull(data.getStringArray());
        assertEquals(3, data.getStringArray().size());
        assertEquals("boat", data.getStringArray().get(0));
        assertEquals("yacht", data.getStringArray().get(1));
        assertEquals("ship", data.getStringArray().get(2));

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
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
        private List<String> m_StringArray;

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

        public List<String> getStringArray()
        {
            return m_StringArray;
        }

        public void setStringArray(List<String> value)
        {
            this.m_StringArray = value;
        }
    }

    private static class TestFieldNameParser extends FieldNameParser<TestData>
    {

        public TestFieldNameParser(JsonParser jsonParser, Logger logger)
        {
            super("TestData", jsonParser, logger);
        }

        @Override
        protected TestData supply()
        {
            return new TestData();
        }

        @Override
        protected void handleFieldName(String fieldName, TestData data)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            m_Parser.nextToken();
            switch(fieldName)
            {
                case "testInt":
                    data.setInt(parseAsIntOrZero(fieldName));
                    break;
                case "testLong":
                    data.setLong(parseAsLongOrZero(fieldName));
                    break;
                case "testDouble":
                    data.setDouble(parseAsDoubleOrZero(fieldName));
                    break;
                case "testBoolean":
                    data.setBoolean(parseAsBooleanOrNull(fieldName));
                    break;
                case "testString":
                    data.setString(parseAsStringOrNull(fieldName));
                    break;
                case "testStringArray":
                    data.setStringArray(parseStringArray(fieldName));
                    break;
                default:
                    m_Logger.error("Invalid fieldName: " + fieldName);
            }
        }
    }
}
