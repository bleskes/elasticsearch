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

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.AutoDetectParseException;

public abstract class FieldNameParser<T>
{
    private final String m_ObjectName;
    protected final JsonParser m_Parser;
    protected final Logger m_Logger;

    public FieldNameParser(String fieldName, JsonParser jsonParser, Logger logger)
    {
        m_ObjectName = fieldName;
        m_Parser = jsonParser;
        m_Logger = logger;
    }

    public void parse(T data) throws AutoDetectParseException, JsonParseException, IOException
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

        token = m_Parser.nextToken();
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

    protected abstract void handleFieldName(String fieldName, T data)
            throws AutoDetectParseException, JsonParseException, IOException;

    protected double parseAsDoubleOrZero(JsonToken token, String fieldName)
            throws JsonParseException, IOException
    {
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT)
        {
            return m_Parser.getDoubleValue();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a double");
        return 0.0;
    }

    protected String parseAsStringOrNull(JsonToken token, String fieldName)
            throws JsonParseException, IOException
    {
        if (token == JsonToken.VALUE_STRING)
        {
            return m_Parser.getText();
        }
        m_Logger.warn("Cannot parse " + fieldName + " : " + m_Parser.getText() + " as a string");
        return null;
    }

    protected Boolean parseAsBooleanOrNull(JsonToken token, String fieldName)
            throws JsonParseException, IOException
    {
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
}
