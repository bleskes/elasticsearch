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
package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;
import com.prelert.utils.json.FieldNameParser;

final class BucketParser extends FieldNameParser<Bucket>
{
    private static final Logger LOGGER = Logger.getLogger(BucketParser.class);

    public BucketParser(JsonParser jsonParser)
    {
        super("Bucket", jsonParser, LOGGER);
    }

    @Override
    protected Bucket supply()
    {
        return new Bucket();
    }

    @Override
    protected void handleFieldName(String fieldName, Bucket bucket) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case Bucket.TIMESTAMP:
            parseTimestamp(bucket, token);
            break;
        case Bucket.ANOMALY_SCORE:
            bucket.setAnomalyScore(parseAsDoubleOrZero(fieldName));
            bucket.setInitialAnomalyScore(bucket.getAnomalyScore());
            break;
        case Bucket.MAX_NORMALIZED_PROBABILITY:
            bucket.setMaxNormalizedProbability(parseAsDoubleOrZero(fieldName));
            break;
        case Bucket.RECORD_COUNT:
            bucket.setRecordCount(parseAsIntOrZero(fieldName));
            break;
        case Bucket.EVENT_COUNT:
            bucket.setEventCount(parseAsLongOrZero(fieldName));
            break;
        case Bucket.IS_INTERIM:
            bucket.setInterim(parseAsBooleanOrNull(fieldName));
            break;
        case Bucket.RECORDS:
            bucket.setRecords(parseRecords(fieldName));
            break;
        case Bucket.BUCKET_INFLUENCERS:
            bucket.setBucketInfluencers(parseBucketInfluencers(fieldName));
            break;
        case Bucket.INFLUENCERS:
            bucket.setInfluencers(parseInfluencers(fieldName));
            break;
        case Bucket.BUCKET_SPAN:
            bucket.setBucketSpan(parseAsLongOrZero(fieldName));
            break;
        case Bucket.PROCESSING_TIME_MS:
            bucket.setProcessingTimeMs(parseAsLongOrZero(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error: unknown field in Bucket %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private void parseTimestamp(Bucket bucket, JsonToken token) throws IOException
    {
        if (token == JsonToken.VALUE_NUMBER_INT)
        {
            // convert seconds to ms
            long val = m_Parser.getLongValue() * 1000;
            bucket.setTimestamp(new Date(val));
        }
        else
        {
            LOGGER.warn("Cannot parse " + Bucket.TIMESTAMP + " : " + m_Parser.getText()
                            + " as a long");
        }
    }

    private List<AnomalyRecord> parseRecords(String fieldName) throws IOException
    {
        List<AnomalyRecord> anomalyRecords = new ArrayList<>();
        parseArray(fieldName, () -> new AnomalyRecordParser(m_Parser).parseJson(), anomalyRecords);
        return anomalyRecords;
    }

    private List<BucketInfluencer> parseBucketInfluencers(String fieldName) throws IOException
    {
        List<BucketInfluencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new BucketInfluencerParser(m_Parser).parseJson(), influencers);
        return influencers;
    }

    private List<Influencer> parseInfluencers(String fieldName) throws IOException
    {
        List<Influencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new InfluencerParser(m_Parser).parseJson(), influencers);
        return influencers;
    }
}
