/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
package com.prelert.job;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.rs.data.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Simple class to parse and store a flush ID.
 */
public class FlushAcknowledgement
{
    /**
     * Field Names
     */
    public static final String FLUSH = "flush";

    private static final Logger LOGGER = Logger.getLogger(FlushAcknowledgement.class);

    private String m_Id;

    public String getId()
    {
        return m_Id;
    }


    public void setId(String id)
    {
        m_Id = id;
    }


    /**
     * Create a new <code>FlushAcknowledgement</code> and populate it from the
     * JSON parser.  The parser must be pointing at the start of the object then
     * all the object's fields are read and if they match the property names the
     * appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new FlushAcknowledgement
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static FlushAcknowledgement parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        FlushAcknowledgement acknowledgement = new FlushAcknowledgement();
        FlushAcknowledgementJsonParser flushAcknowledgementJsonParser =
                new FlushAcknowledgementJsonParser(parser, LOGGER);
        flushAcknowledgementJsonParser.parse(acknowledgement);
        return acknowledgement;
    }


    /**
     * Create a new <code>FlushAcknowledgement</code> and populate it from the
     * JSON parser.  The parser must be pointing at the first token inside the
     * object.  It is assumed that prior code has validated that the previous
     * token was the start of an object.  Then all the object's fields are read
     * and if they match the property names the appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new FlushAcknowledgement
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static FlushAcknowledgement parseJsonAfterStartObject(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        FlushAcknowledgement acknowledgement = new FlushAcknowledgement();
        FlushAcknowledgementJsonParser flushAcknowledgementJsonParser =
                new FlushAcknowledgementJsonParser(parser, LOGGER);
        flushAcknowledgementJsonParser.parseAfterStartObject(acknowledgement);
        return acknowledgement;
    }

    private static class FlushAcknowledgementJsonParser extends FieldNameParser<FlushAcknowledgement>
    {

        public FlushAcknowledgementJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("FlushAcknowledgement", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, FlushAcknowledgement ack)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case FLUSH:
                ack.setId(parseAsStringOrNull(token, fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in FlushAcknowledgement %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}

