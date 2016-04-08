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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Base class that allows parsing of simple JSON objects given a JsonParser
 * that either points to the start object, or right after it. The class defines
 * an abstract method that allows clients to specify how the fieldNames should be parsed,
 * without having to duplicate the rest of the parsing process. It also provide helper
 * methods for parsing the next token as various data types.
 */
public abstract class FieldNameParser<T>
{
    protected interface ElementParser<T>
    {
        T parse() throws JsonParseException, IOException, AutoDetectParseException;
    }

    private final String m_ObjectName;
    protected final JsonParser m_Parser;
    protected final Logger m_Logger;

    public FieldNameParser(String fieldName, JsonParser jsonParser, Logger logger)
    {
        m_ObjectName = fieldName;
        m_Parser = jsonParser;
        m_Logger = logger;
    }

    /**
     * Creates a new object T and populates it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @return The populated data
     * @throws AutoDetectParseException
     */
    public T parseJson() throws AutoDetectParseException
    {
        T result = supply();
        try
        {
            parse(result);
        }
        catch (IOException e)
        {
            throw new AutoDetectParseException(e);
        }
        return result;
    }

    /**
     * Creates a new object T and populates it from the JSON parser.
     * The parser must be pointing at the first token inside the object.  It
     * is assumed that prior code has validated that the previous token was
     * the start of an object.  Then all the object's fields are read and if
     * they match the property names the appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @return The populated data
     * @throws AutoDetectParseException
     */
    public T parseJsonAfterStartObject() throws AutoDetectParseException
    {
        T result = supply();
        try
        {
            parseAfterStartObject(result);
        }
        catch (IOException e)
        {
            throw new AutoDetectParseException(e);
        }
        return result;
    }

    private void parse(T data) throws IOException
    {
        JsonToken token = m_Parser.getCurrentToken();
        if (JsonToken.START_OBJECT != token)
        {
            String msg = String.format(
                    "Cannot parse %s. First token '%s', is not the start object token",
                    m_ObjectName, m_Parser.getText());
            m_Logger.error(msg);
            throw new AutoDetectParseException(msg);
        }
        m_Parser.nextToken();
        parseAfterStartObject(data);
    }

    private void parseAfterStartObject(T data) throws IOException
    {
        JsonToken token = m_Parser.getCurrentToken();
        while (token != JsonToken.END_OBJECT)
        {
            switch(token)
            {
                case START_OBJECT:
                    m_Logger.error(String.format("Start object parsed in %s", m_ObjectName));
                    break;
                case END_OBJECT:
                    m_Logger.error(String.format("End object parsed in %s", m_ObjectName));
                    break;
                case FIELD_NAME:
                    String fieldName = m_Parser.getCurrentName();
                    handleFieldName(fieldName, data);
                    break;
                default:
                    m_Logger.warn(String.format(
                            "Parsing error: Only simple fields expected in %s not %s",
                            m_ObjectName, token));
                    break;
            }

            token = m_Parser.nextToken();
        }
    }

    /**
     * Supply a new object to be populated from the parser
     */
    protected abstract T supply();

    protected abstract void handleFieldName(String fieldName, T data) throws IOException;

    protected int parseAsIntOrZero(String fieldName) throws IOException
    {
        if (m_Parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT)
        {
            return m_Parser.getIntValue();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as an int");
        return 0;
    }

    protected long parseAsLongOrZero(String fieldName) throws IOException
    {
        if (m_Parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT)
        {
            return m_Parser.getLongValue();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a long");
        return 0;
    }

    protected double parseAsDoubleOrZero(String fieldName) throws IOException
    {
        JsonToken token = m_Parser.getCurrentToken();
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
        {
            return m_Parser.getDoubleValue();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a double");
        return 0.0;
    }

    protected String parseAsStringOrNull(String fieldName) throws IOException
    {
        if (m_Parser.getCurrentToken() == JsonToken.VALUE_STRING)
        {
            return m_Parser.getText();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a string");
        return null;
    }

    protected Boolean parseAsBooleanOrNull(String fieldName) throws IOException
    {
        JsonToken token = m_Parser.getCurrentToken();
        if (token == JsonToken.VALUE_TRUE)
        {
            return true;
        }
        if (token == JsonToken.VALUE_FALSE)
        {
            return false;
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a boolean");
        return null;
    }

    protected <E> void parseArray(String fieldName, ElementParser<E> elementParser,
            Collection<E> result) throws IOException
    {
        JsonToken token = m_Parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY)
        {
            String msg = "Invalid value Expecting an array of " + fieldName;
            m_Logger.warn(msg);
            throw new AutoDetectParseException(msg);
        }

        token = m_Parser.nextToken();
        while (token != JsonToken.END_ARRAY)
        {
            result.add(elementParser.parse());
            token = m_Parser.nextToken();
        }
    }

    protected double[] parsePrimitiveDoubleArray(String fieldName) throws IOException
    {
        List<Double> list = new ArrayList<>();
        parseArray(fieldName, () -> parseAsDoubleOrZero(fieldName), list);
        return list.stream().mapToDouble(d -> d).toArray();
    }
}
