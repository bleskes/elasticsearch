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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Detector;
import com.prelert.job.results.Influencer;
import com.prelert.utils.json.AutoDetectParseException;
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
    protected void postProcess(Bucket bucket)
    {
        setRecordCountToSumOfDetectorsRecords(bucket);
    }

    @Override
    protected void handleFieldName(String fieldName, Bucket bucket)
            throws AutoDetectParseException, JsonParseException, IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case Bucket.TIMESTAMP:
            parseTimestamp(bucket, token);
            break;
        case Bucket.ANOMALY_SCORE:
            bucket.setAnomalyScore(parseAsDoubleOrZero(fieldName));
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
            bucket.setInterim(parseAsBooleanOrNull( fieldName));
            break;
        case Bucket.DETECTORS:
            bucket.setDetectors(parseDetectors(fieldName));
            break;
        case Bucket.BUCKET_INFLUENCERS:
            bucket.setBucketInfluencers(parseBucketInfluencers(fieldName));
            break;
        case Bucket.INFLUENCERS:
            bucket.setInfluencers(parseInfluencers(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in Bucket %s:%s",
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

    private List<Detector> parseDetectors(String fieldName) throws AutoDetectParseException,
            IOException, JsonParseException
    {
        List<Detector> detectors = new ArrayList<>();
        parseArray(fieldName, () -> new DetectorParser(m_Parser).parseJson(), detectors);
        return detectors;
    }

    private List<BucketInfluencer> parseBucketInfluencers(String fieldName)
            throws AutoDetectParseException, IOException, JsonParseException
    {
        List<BucketInfluencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new BucketInfluencerParser(m_Parser).parseJson(), influencers);
        return influencers;
    }

    private List<Influencer> parseInfluencers(String fieldName)
            throws AutoDetectParseException, IOException, JsonParseException
    {
        List<Influencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new InfluencerParser(m_Parser).parseJson(), influencers);
        return influencers;
    }

    /** Set the record count to what was actually read */
    private static void setRecordCountToSumOfDetectorsRecords(Bucket bucket)
    {
        int acc = 0;
        for (Detector d : bucket.getDetectors())
        {
            acc += d.getRecords().size();
        }
        bucket.setRecordCount(acc);
    }
}
