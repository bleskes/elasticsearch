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
import com.prelert.job.results.BucketInfluencer;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Static methods for {@linkplain BucketInfluencerParser}
 *
 */
public final class BucketInfluencerParser
{
    private static final Logger LOGGER = Logger.getLogger(BucketInfluencerParser.class);

    private BucketInfluencerParser()
    {
    }

    /**
     * Create a new <code>BucketInfluencer</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names then the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The parsed {@code BucketInfluencer}
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static BucketInfluencer parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        BucketInfluencer influencer = new BucketInfluencer();
        BucketInfluencerJsonParser influencerJsonParser = new BucketInfluencerJsonParser(parser, LOGGER);
        influencerJsonParser.parse(influencer);
        return influencer;
    }

    private static class BucketInfluencerJsonParser extends FieldNameParser<BucketInfluencer>
    {

        public BucketInfluencerJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("BucketInfluencer", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, BucketInfluencer influencer)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case BucketInfluencer.PROBABILITY:
                influencer.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case BucketInfluencer.INITIAL_ANOMALY_SCORE:
                influencer.setInitialAnomalyScore(parseAsDoubleOrZero(fieldName));
                influencer.setAnomalyScore(influencer.getInitialAnomalyScore());
                break;
            case BucketInfluencer.RAW_ANOMALY_SCORE:
                influencer.setRawAnomalyScore(parseAsDoubleOrZero(fieldName));
                break;
            case BucketInfluencer.INFLUENCER_FIELD_NAME:
                influencer.setInfluencerFieldName(parseAsStringOrNull(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in BucketInfluencer %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}

