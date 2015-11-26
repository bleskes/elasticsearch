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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.Influencer;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Static methods for {@linkplain InfluencerParser}
 *
 */
public class InfluencerParser
{
    private static final Logger LOGGER = Logger.getLogger(InfluencerParser.class);

    private InfluencerParser()
    {
    }

    /**
     * Create a new <code>Influencer</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The parsed {@code Influencer}
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Influencer parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Influencer influencer = new Influencer();
        InfluencerJsonParser influencerJsonParser = new InfluencerJsonParser(parser, LOGGER);
        influencerJsonParser.parse(influencer);

        return influencer;
    }

    private static class InfluencerJsonParser extends FieldNameParser<Influencer>
    {

        public InfluencerJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Influencer", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, Influencer influencer)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case Influencer.PROBABILITY:
                influencer.setProbability(parseAsDoubleOrZero( fieldName));
                break;
            case Influencer.INITIAL_ANOMALY_SCORE:
                influencer.setInitialAnomalyScore(parseAsDoubleOrZero(fieldName));
                influencer.setAnomalyScore(influencer.getInitialAnomalyScore());
                break;
            case Influencer.INFLUENCER_FIELD_NAME:
                influencer.setInfluencerFieldName(parseAsStringOrNull(fieldName));
                break;
            case Influencer.INFLUENCER_VALUE_NAME:
                influencer.setInfluencerFieldValue(parseAsStringOrNull(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Influencer %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}

