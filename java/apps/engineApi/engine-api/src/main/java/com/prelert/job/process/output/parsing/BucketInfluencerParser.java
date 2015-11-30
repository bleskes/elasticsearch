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

final class BucketInfluencerParser extends FieldNameParser<BucketInfluencer>
{
    private static final Logger LOGGER = Logger.getLogger(BucketInfluencerParser.class);

    public BucketInfluencerParser(JsonParser jsonParser)
    {
        super("BucketInfluencer", jsonParser, LOGGER);
    }

    @Override
    protected BucketInfluencer supply()
    {
        return new BucketInfluencer();
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

