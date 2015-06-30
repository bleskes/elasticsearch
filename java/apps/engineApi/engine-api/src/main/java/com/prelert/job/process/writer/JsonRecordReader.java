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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.process.exceptions.MalformedJsonException;

class JsonRecordReader
{
    private static final int PARSE_ERRORS_LIMIT = 1000;

    private final JsonParser m_Parser;
    private final Map<String, Integer> m_FieldMap;
    private final Logger m_Logger;
    private int m_NestedLevel;
    private long m_FieldCount;
    private Deque<String> m_NestedFields;
    private String m_NestedSuffix;

    /**
     * Create a reader that parses the mapped fields from JSON.
     *
     * @param parser The JSON parser
     * @param fieldMap Map to field name to record array index position
     * @param logger
     */
    JsonRecordReader(JsonParser parser, Map<String, Integer> fieldMap, Logger logger)
    {
        m_Parser = Objects.requireNonNull(parser);
        m_FieldMap = Objects.requireNonNull(fieldMap);
        m_Logger = Objects.requireNonNull(logger);
    }

    /**
     * Read the JSON object and write to the record array.
     * Nested objects are flattened with the field names separated by
     * a '.'.
     * e.g. for a record with a nested 'tags' object:
     *  "{"name":"my.test.metric1","tags":{"tag1":"blah","tag2":"boo"},"time":1350824400,"value":12345.678}"
     * use 'tags.tag1' to reference the tag1 field in the nested object
     *
     * Array fields in the JSON are ignored
     *
     * @param record Read fields are written to this array. This array is first filled with empty
     * strings and will never contain a <code>null</code>
     * @param gotFields boolean array each element is true if that field
     * was read
     *
     * @return The number of fields in the JSON doc or -1 if nothing was read
     * @throws IOException
     * @throws MalformedJsonException
     */
    long read(String[] record, boolean[] gotFields) throws IOException, MalformedJsonException
    {
        Arrays.fill(gotFields, false);
        Arrays.fill(record, "");
        m_FieldCount = 0;
        boolean readSomething = false;
        clearNestedLevel();

        JsonToken token = tryNextTokenOrReadToEndOnError();
        while (!(token == JsonToken.END_OBJECT && m_NestedLevel == 0))
        {
            if (token == null)
            {
                break;
            }

            readSomething = true;

            if (token == JsonToken.END_OBJECT)
            {
                m_NestedLevel--;
                String objectFieldName = m_NestedFields.pop();

                int lastIndex = m_NestedSuffix.length() - objectFieldName.length() -1;
                m_NestedSuffix = m_NestedSuffix.substring(0, lastIndex);
            }
            else if (token == JsonToken.FIELD_NAME)
            {
                parseFieldValuePair(record, gotFields);
            }

            token = tryNextTokenOrReadToEndOnError();
        }

        if (readSomething)
        {
            return m_FieldCount;
        }
        else
        {
            return -1;
        }
    }

    private void clearNestedLevel()
    {
        m_NestedLevel = 0;
        m_NestedFields = new ArrayDeque<String>();
        m_NestedSuffix = "";
    }

    private JsonToken tryNextTokenOrReadToEndOnError() throws IOException, MalformedJsonException
    {
        try
        {
            return m_Parser.nextToken();
        }
        catch (JsonParseException e)
        {
            m_Logger.warn("Attempting to recover from malformed JSON data.", e);
            for (int i = 0; i <= m_NestedLevel; i++)
            {
                readToEndOfObject();
            }
            clearNestedLevel();
        }
        return null;
    }

    private void readToEndOfObject() throws IOException, MalformedJsonException
    {
        JsonToken token = null;
        int errorCounter = 0;
        do
        {
            try
            {
                token = m_Parser.nextToken();
            }
            catch (JsonParseException e)
            {
                errorCounter++;
                if (errorCounter >= PARSE_ERRORS_LIMIT)
                {
                    m_Logger.error("Failed to recover from malformed JSON data.", e);
                    throw new MalformedJsonException(e);
                }
            }
        }
        while (token != JsonToken.END_OBJECT);
    }

    private void parseFieldValuePair(String[] record, boolean[] gotFields)
            throws IOException, MalformedJsonException
    {
        String fieldName = m_Parser.getCurrentName();
        JsonToken token = tryNextTokenOrReadToEndOnError();

        if (token == null)
        {
            return;
        }

        if (token == JsonToken.START_OBJECT)
        {
            m_NestedLevel++;
            m_NestedFields.push(fieldName);
            m_NestedSuffix = m_NestedSuffix + fieldName + ".";
        }
        else if (token == JsonToken.START_ARRAY)
        {
            // consume the whole array but do nothing with it
            while (token != JsonToken.END_ARRAY)
            {
                token = tryNextTokenOrReadToEndOnError();
            }
            m_Logger.warn("Ignoring array field");
        }
        else
        {
            ++m_FieldCount;

            String fieldValue = m_Parser.getText();

            Integer index = m_FieldMap.get(m_NestedSuffix + fieldName);
            if (index != null)
            {
                record[index] = fieldValue;
                gotFields[index] = true;
            }
        }
    }
}
