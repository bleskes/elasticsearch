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
package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.Influence;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;


public class InfluenceParser
{
    private static final Logger LOGGER = Logger.getLogger(InfluenceParser.class);

    private InfluenceParser()
    {
    }

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
            influence.setInfluencerFieldName(fieldName);
            m_Parser.nextToken();
            influence.setInfluencerFieldValues(parseValues(fieldName));

            influences.add(influence);
        }

        private List<String> parseValues(String fieldName) throws AutoDetectParseException,
                IOException, JsonParseException
        {
            List<String> influenceValues = new ArrayList<>();
            parseArray(fieldName, () -> parseAsStringOrNull(fieldName), influenceValues);
            return influenceValues;
        }
    }
}

