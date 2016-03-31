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
package com.prelert.job.process.writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.process.exceptions.MalformedJsonException;

public class SimpleJsonRecordReaderTest
{
    @Test
    public void testRead() throws JsonParseException, IOException, MalformedJsonException
    {
        String data = "{\"a\":10, \"b\":20, \"c\":30}\n{\"b\":21, \"a\":11, \"c\":31}\n";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = createFieldMap();

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(3, reader.read(record, gotFields));
        assertEquals("10", record[0]);
        assertEquals("20", record[1]);
        assertEquals("30", record[2]);

        assertEquals(3, reader.read(record, gotFields));
        assertEquals("11", record[0]);
        assertEquals("21", record[1]);
        assertEquals("31", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    @Test
    public void testRead_GivenNestedField() throws JsonParseException, IOException, MalformedJsonException
    {
        String data = "{\"a\":10, \"b\":20, \"c\":{\"d\":30, \"e\":40}}";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = new HashMap<>();
        fieldMap.put("a", 0);
        fieldMap.put("b", 1);
        fieldMap.put("c.e", 2);

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(4, reader.read(record, gotFields));
        assertEquals("10", record[0]);
        assertEquals("20", record[1]);
        assertEquals("40", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    @Test
    public void testRead_GivenSingleValueArrays() throws JsonParseException, IOException, MalformedJsonException
    {
        String data = "{\"a\":[10], \"b\":20, \"c\":{\"d\":30, \"e\":[40]}}";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = new HashMap<>();
        fieldMap.put("a", 0);
        fieldMap.put("b", 1);
        fieldMap.put("c.e", 2);

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(4, reader.read(record, gotFields));
        assertEquals("10", record[0]);
        assertEquals("20", record[1]);
        assertEquals("40", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    @Test
    public void testRead_GivenMultiValueArrays() throws JsonParseException, IOException, MalformedJsonException
    {
        String data = "{\"a\":[10, 11], \"b\":20, \"c\":{\"d\":30, \"e\":[40, 50]}}";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = new HashMap<>();
        fieldMap.put("a", 0);
        fieldMap.put("b", 1);
        fieldMap.put("c.e", 2);

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(2, reader.read(record, gotFields));
        assertEquals("", record[0]);
        assertEquals("20", record[1]);
        assertEquals("", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    /**
     * There's a problem with the parser where in this case it skips over the first 2 records
     * instead of to the end of the first record which is invalid json.
     * This means we miss the next record after a bad one.
     */
    @Test
    public void testRead_RecoverFromBadJson() throws JsonParseException, IOException, MalformedJsonException
    {
        // no opening '{'
        String data = "\"a\":10, \"b\":20, \"c\":30}\n{\"b\":21, \"a\":11, \"c\":31}\n{\"c\":32, \"b\":22, \"a\":12}";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = createFieldMap();

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(0, reader.read(record, gotFields));
        assertEquals(3, reader.read(record, gotFields));
        assertEquals("12", record[0]);
        assertEquals("22", record[1]);
        assertEquals("32", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    @Test
    public void testRead_RecoverFromBadNestedJson() throws JsonParseException, IOException, MalformedJsonException
    {
        // nested object 'd' is missing a ','
        String data = "{\"a\":10, \"b\":20, \"c\":30}\n" +
                        "{\"b\":21, \"d\" : {\"ee\": 1 \"ff\":0}, \"a\":11, \"c\":31}";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = createFieldMap();

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        // reads first object ok
        assertEquals(3, reader.read(record, gotFields));
        // skips to the end of the 2nd after reading 2 fields
        assertEquals(2, reader.read(record, gotFields));
        assertEquals("", record[0]);
        assertEquals("21", record[1]);
        assertEquals("", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }


    @Test(expected=MalformedJsonException.class)
    public void testRead_HitParseErrorsLimit() throws JsonParseException, IOException, MalformedJsonException
    {
        // missing a ':'
        String format = "{\"a\":1%1$d, \"b\"2%1$d, \"c\":3%1$d}\n";
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<SimpleJsonRecordReader.PARSE_ERRORS_LIMIT; i++)
        {
            builder.append(String.format(format, i));
        }

        JsonParser parser = createParser(builder.toString());
        Map<String, Integer> fieldMap = createFieldMap();

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "", mock(Logger.class));
        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        for (int i=0; i<SimpleJsonRecordReader.PARSE_ERRORS_LIMIT; i++)
        {
            // this should throw after PARSE_ERRORS_LIMIT errors
            reader.read(record, gotFields);
        }
    }

    @Test
    public void testRead_GivenDataEmbeddedInSource() throws JsonParseException, IOException,
            MalformedJsonException
    {
        String data = "{\"took\": 1,\"hits\":{\"total\":1,\"hits\":["
                + "{\"_index\":\"foo\",\"_source\":{\"a\":1,\"b\":2,\"c\":3}},"
                + "{\"_index\":\"foo\",\"_source\":{\"a\":4,\"b\":5,\"c\":6}}"
                + "]}}\n";
        JsonParser parser = createParser(data);
        Map<String, Integer> fieldMap = createFieldMap();

        SimpleJsonRecordReader reader = new SimpleJsonRecordReader(parser, fieldMap, "_source", mock(Logger.class));

        String record [] = new String[3];
        boolean gotFields [] = new boolean[3];

        assertEquals(3, reader.read(record, gotFields));
        assertEquals("1", record[0]);
        assertEquals("2", record[1]);
        assertEquals("3", record[2]);

        assertEquals(3, reader.read(record, gotFields));
        assertEquals("4", record[0]);
        assertEquals("5", record[1]);
        assertEquals("6", record[2]);

        assertEquals(-1, reader.read(record, gotFields));
    }

    private JsonParser createParser(String input) throws JsonParseException, IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

    private Map<String, Integer> createFieldMap()
    {
        Map<String, Integer> fieldMap = new HashMap<>();
        fieldMap.put("a", 0);
        fieldMap.put("b", 1);
        fieldMap.put("c", 2);
        return fieldMap;
    }

}
