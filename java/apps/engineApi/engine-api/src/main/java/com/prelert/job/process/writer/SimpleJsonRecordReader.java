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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.process.exceptions.MalformedJsonException;

class SimpleJsonRecordReader extends AbstractJsonRecordReader
{
    private Deque<String> m_NestedFields;
    private String m_NestedPrefix;

    /**
     * Create a reader that parses the mapped fields from JSON.
     *
     * @param parser The JSON parser
     * @param fieldMap Map to field name to record array index position
     * @param recordHoldingField
     * @param logger
     */
    SimpleJsonRecordReader(JsonParser parser, Map<String, Integer> fieldMap, String recordHoldingField,
            Logger logger)
    {
        super(parser, fieldMap, recordHoldingField, logger);
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
     * because the end of the stream was reached
     * @throws IOException
     * @throws MalformedJsonException
     */
    @Override
    public long read(String[] record, boolean[] gotFields) throws IOException, MalformedJsonException
    {
        initArrays(record, gotFields);
        m_FieldCount = 0;
        clearNestedLevel();
        consumeToRecordHoldingField();

        JsonToken token = tryNextTokenOrReadToEndOnError();
        while (!(token == JsonToken.END_OBJECT && m_NestedLevel == 0))
        {
            if (token == null)
            {
                break;
            }

            if (token == JsonToken.END_OBJECT)
            {
                --m_NestedLevel;
                String objectFieldName = m_NestedFields.pop();

                int lastIndex = m_NestedPrefix.length() - objectFieldName.length() - 1;
                m_NestedPrefix = m_NestedPrefix.substring(0, lastIndex);
            }
            else if (token == JsonToken.FIELD_NAME)
            {
                parseFieldValuePair(record, gotFields);
            }

            token = tryNextTokenOrReadToEndOnError();
        }

        // null token means EOF
        if (token == null)
        {
            return -1;
        }
        return m_FieldCount;
    }

    protected void clearNestedLevel()
    {
        m_NestedLevel = 0;
        m_NestedFields = new ArrayDeque<String>();
        m_NestedPrefix = "";
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
            ++m_NestedLevel;
            m_NestedFields.push(fieldName);
            m_NestedPrefix = m_NestedPrefix + fieldName + ".";
        }
        else
        {
            if (token == JsonToken.START_ARRAY || token.isScalarValue())
            {
                ++m_FieldCount;

                // Only do the donkey work of converting the field value to a
                // string if we need it
                Integer index = m_FieldMap.get(m_NestedPrefix + fieldName);
                if (index != null)
                {
                    record[index] = parseSingleFieldValue(token);
                    gotFields[index] = true;
                }
                else
                {
                    skipSingleFieldValue(token);
                }
            }
        }
    }

    private String parseSingleFieldValue(JsonToken token)
            throws IOException, MalformedJsonException
    {
        if (token == JsonToken.START_ARRAY)
        {
            // Convert any scalar values in the array to a comma delimited
            // string.  (Arrays of more complex objects are ignored.)
            StringBuilder strBuilder = new StringBuilder();
            boolean needComma = false;
            while (token != JsonToken.END_ARRAY)
            {
                token = tryNextTokenOrReadToEndOnError();
                if (token.isScalarValue())
                {
                    if (needComma)
                    {
                        strBuilder.append(',');
                    }
                    else
                    {
                        needComma = true;
                    }
                    strBuilder.append(tokenToString(token));
                }
            }

            return strBuilder.toString();
        }

        return tokenToString(token);
    }

    private void skipSingleFieldValue(JsonToken token)
            throws IOException, MalformedJsonException
    {
        // Scalar values don't need any extra skip code
        if (token == JsonToken.START_ARRAY)
        {
            // Consume the whole array but do nothing with it
            int arrayDepth = 1;
            do
            {
                token = tryNextTokenOrReadToEndOnError();
                if (token == JsonToken.END_ARRAY)
                {
                    --arrayDepth;
                }
                else if (token == JsonToken.START_ARRAY)
                {
                    ++arrayDepth;
                }
            }
            while (token != null && arrayDepth > 0);
        }
    }

    /**
     * Get the text representation of the current token unless it's a null.
     * Nulls are replaced with empty strings to match the way the rest of the
     * product treats them (which in turn is shaped by the fact that CSV
     * cannot distinguish empty string and null).
     */
    private String tokenToString(JsonToken token) throws IOException
    {
        if (token == null || token == JsonToken.VALUE_NULL)
        {
            return "";
        }
        return m_Parser.getText();
    }
}
