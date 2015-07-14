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
package com.prelert.job.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Static methods or Influences
 *
 */
public class Influences
{
    private static final Logger LOGGER = Logger.getLogger(Influences.class);

    /**
     * Create a new <code>List&ltInfluence&gt</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return List of {@linkplain Influence}
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static List<Influence> parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        List<Influence> records = new ArrayList<>();
        InfluencesJsonParser influencesJsonParser = new InfluencesJsonParser(parser, LOGGER);
        influencesJsonParser.parse(records);
        return records;
    }

    private static class InfluencesJsonParser extends FieldNameParser<List<Influence>>
    {

        public InfluencesJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Influences", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, List<Influence> influences)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            Influence influence = new Influence();
            influence.setField(fieldName);
            JsonToken token = m_Parser.nextToken();
            parseScores(token, influence);

            influences.add(influence);
        }

        private void parseScores(JsonToken token, Influence influence)
                throws AutoDetectParseException, IOException, JsonParseException
        {
            if (token != JsonToken.START_ARRAY)
            {
                String msg = "Invalid value Expecting an array of influence scores";
                LOGGER.warn(msg);
                throw new AutoDetectParseException(msg);
            }

            token = m_Parser.nextToken();
            while (token != JsonToken.END_ARRAY)
            {
                InfluenceScore score = parseScore(token);
                influence.addScore(score);

                token = m_Parser.nextToken();
            }
        }

        /**
         * The parser should be pointing at the start of an object and
         * will be pointing at the end of the object when this function returns
         * @param token
         * @return
         * @throws IOException
         */
        private InfluenceScore parseScore(JsonToken token) throws IOException
        {
            InfluenceScore is = new InfluenceScore();
            if (token == JsonToken.START_OBJECT)
            {
                token = m_Parser.nextToken();
            }

            if (token == JsonToken.FIELD_NAME)
            {
                String fieldValue = m_Parser.getCurrentName();
                is.setFieldValue(fieldValue);
                is.setInfluence(parseAsDoubleOrZero(m_Parser.nextToken(), fieldValue));
            }
            else
            {
                LOGGER.error("Expected a field name parsing InfluenceScore not " + token);
            }

            token = m_Parser.nextToken();
            while (token != JsonToken.END_OBJECT)
            {
                LOGGER.warn("Expected end object parsing InfluenceScore not " + token);
                token = m_Parser.nextToken();
            }

            return is;
        }
    }
}

