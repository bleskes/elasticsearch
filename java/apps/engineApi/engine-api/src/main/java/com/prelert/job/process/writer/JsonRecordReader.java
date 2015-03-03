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

class JsonRecordReader
{

    private final JsonParser m_Parser;
    private final Map<String, Integer> m_FieldMap;
    private final Logger m_Logger;
    private int m_NestedLevel;

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
     * @param record Read fields are written to this array
     * @param gotFields boolean array each element is true if that field
     * was read
     *
     * @return The number of fields in the JSON doc
     * @throws IOException
     * @throws JsonParseException
     */
    public long read(String[] record, boolean[] gotFields) throws IOException
    {
        Arrays.fill(gotFields, false);
        Arrays.fill(record, "");

        m_NestedLevel = 0;
        Deque<String> stack = new ArrayDeque<String>();

        long fieldCount = 0;
        String nestedSuffix = "";

        JsonToken token = tryNextTokenOrReadToEndOnError();
        while (!(token == JsonToken.END_OBJECT && m_NestedLevel == 0))
        {
            if (token == null)
            {
                break;
            }
            if (token == JsonToken.END_OBJECT)
            {
                m_NestedLevel--;
                String objectFieldName = stack.pop();

                int lastIndex = nestedSuffix.length() - objectFieldName.length() -1;
                nestedSuffix = nestedSuffix.substring(0, lastIndex);
            }
            else if (token == JsonToken.FIELD_NAME)
            {
                String fieldName = m_Parser.getCurrentName();
                token = tryNextTokenOrReadToEndOnError();

                if (token == null)
                {
                    break;
                }
                else if (token == JsonToken.START_OBJECT)
                {
                    m_NestedLevel++;
                    stack.push(fieldName);

                    nestedSuffix = nestedSuffix + fieldName + ".";
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
                    ++fieldCount;

                    String fieldValue = m_Parser.getText();

                    Integer index = m_FieldMap.get(nestedSuffix + fieldName);
                    if (index != null)
                    {
                        record[index] = fieldValue;
                        gotFields[index] = true;
                    }
                }
            }

            token = tryNextTokenOrReadToEndOnError();
        }

        return fieldCount;
    }

    private JsonToken tryNextTokenOrReadToEndOnError() throws IOException
    {
        try
        {
            return m_Parser.nextToken();
        }
        catch (JsonParseException e)
        {
            for (int i = 0; i <= m_NestedLevel; i++)
            {
                readToEndOfObject();
            }
        }
        return null;
    }

    private void readToEndOfObject() throws IOException
    {
        JsonToken token = null;
        do
        {
            try
            {
                token = m_Parser.nextToken();
            }
            catch (JsonParseException e)
            {
            }
        }
        while (token != JsonToken.END_OBJECT);
    }
}
