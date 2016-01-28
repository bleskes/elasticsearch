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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.process.exceptions.MalformedJsonException;

abstract class AbstractJsonRecordReader implements JsonRecordReader
{
    static final int PARSE_ERRORS_LIMIT = 100;

    protected final JsonParser m_Parser;
    protected final Map<String, Integer> m_FieldMap;
    protected final String m_RecordHoldingField;
    protected final Logger m_Logger;
    protected int m_NestedLevel;
    protected long m_FieldCount;
    protected int m_ErrorCounter;

    /**
     * Create a reader that parses the mapped fields from JSON.
     *
     * @param parser The JSON parser
     * @param fieldMap Map to field name to record array index position
     * @param recordHoldingField
     * @param logger
     */
    AbstractJsonRecordReader(JsonParser parser, Map<String, Integer> fieldMap, String recordHoldingField,
            Logger logger)
    {
        m_Parser = Objects.requireNonNull(parser);
        m_FieldMap = Objects.requireNonNull(fieldMap);
        m_RecordHoldingField = Objects.requireNonNull(recordHoldingField);
        m_Logger = Objects.requireNonNull(logger);
    }

    protected void consumeToField(String field) throws MalformedJsonException, IOException
    {
        if (field == null || field.isEmpty())
        {
            return;
        }
        JsonToken token = null;
        while ((token = tryNextTokenOrReadToEndOnError()) != null)
        {
            if (token == JsonToken.FIELD_NAME
                    && m_Parser.getCurrentName().equals(field))
            {
                tryNextTokenOrReadToEndOnError();
                return;
            }
        }
    }

    protected void consumeToRecordHoldingField() throws MalformedJsonException, IOException
    {
        consumeToField(m_RecordHoldingField);
    }

    protected void initArrays(String[] record, boolean[] gotFields)
    {
        Arrays.fill(gotFields, false);
        Arrays.fill(record, "");
    }

    /**
     * Returns null at the EOF or the next token
     * @return
     * @throws IOException
     * @throws MalformedJsonException
     */
    protected JsonToken tryNextTokenOrReadToEndOnError() throws IOException, MalformedJsonException
    {
        try
        {
            return m_Parser.nextToken();
        }
        catch (JsonParseException e)
        {
            m_Logger.warn("Attempting to recover from malformed JSON data.", e);
            for (int i = 0; i <= m_NestedLevel; ++i)
            {
                readToEndOfObject();
            }
            clearNestedLevel();
        }

        return m_Parser.getCurrentToken();
    }

    protected abstract void clearNestedLevel();

    /**
     * In some cases the parser doesn't recognise the '}' of a badly formed
     * JSON document and so may skip to the end of the second document. In this
     * case we lose an extra record.
     *
     * @throws IOException
     * @throws MalformedJsonException
     */
    protected void readToEndOfObject() throws IOException, MalformedJsonException
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
                ++m_ErrorCounter;
                if (m_ErrorCounter >= PARSE_ERRORS_LIMIT)
                {
                    m_Logger.error("Failed to recover from malformed JSON data.", e);
                    throw new MalformedJsonException(e);
                }
            }
        }
        while (token != JsonToken.END_OBJECT);
    }
}
