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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

public class BucketParserTest
{
    private static final double EPSILON = 0.000001;

    @Test
    public void testParseJson() throws IOException
    {
        String json = "{"
                + "\"timestamp\" : 1369437000,"
                + "\"maxNormalizedProbability\" : 2.0,"
                + "\"anomalyScore\" : 50.0,"
                + "\"eventCount\" : 1693,"
                + "\"recordCount\" : 2,"
                + "\"isInterim\" : false,"
                + "\"bucketSpan\" : 5580,"
                + "\"bucketInfluencers\": ["
                +     "{\"influencerFieldName\":\"bucketTime\",\"probability\":0.03,\"rawAnomalyScore\":0.05,\"initialAnomalyScore\":95.4},"
                +     "{\"influencerFieldName\":\"user\",\"probability\":0.02,\"rawAnomalyScore\":0.13,\"initialAnomalyScore\":33.2}"
                + "],"
                + "\"influencers\" : ["
                +     "{\"probability\":0.9,\"initialAnomalyScore\":97.1948,\"influencerFieldName\":\"src_ip\",\"influencerFieldValue\":\"23.28.243.150\"},"
                +     "{\"probability\":0.4,\"initialAnomalyScore\":12.1948,\"influencerFieldName\":\"dst_ip\",\"influencerFieldValue\":\"23.28.243.1\"}"
                +   "],"
                + "\"records\" : ["
                +     "{\"detectorIndex\":0,\"probability\":0.03,\"typical\":[42.0],\"actual\":[0.2]},"
                +     "{\"detectorIndex\":1,\"probability\":0.01,\"typical\":[60.0],\"actual\":[0.01]}"
                + "]"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        Bucket b = new BucketParser(parser).parseJson();
        assertEquals(1369437000000l, b.getTimestamp().getTime());
        assertEquals(2.0, b.getMaxNormalizedProbability(), EPSILON);
        assertEquals(50.0, b.getAnomalyScore(), EPSILON);
        assertEquals(50.0, b.getInitialAnomalyScore(), EPSILON);
        assertEquals(2, b.getRecordCount());
        assertEquals(1693, b.getEventCount());
        assertFalse(b.isInterim());
        assertEquals(5580l, b.getBucketSpan());

        List<AnomalyRecord> records = b.getRecords();
        assertEquals(2, records.size());
        assertEquals(0, records.get(0).getDetectorIndex());
        assertEquals(0.03, records.get(0).getProbability(), EPSILON);
        assertEquals(42.0, records.get(0).getTypical()[0], EPSILON);
        assertEquals(0.2, records.get(0).getActual()[0], EPSILON);
        assertEquals(1, records.get(1).getDetectorIndex());
        assertEquals(0.01, records.get(1).getProbability(), EPSILON);
        assertEquals(60.0, records.get(1).getTypical()[0], EPSILON);
        assertEquals(0.01, records.get(1).getActual()[0], EPSILON);

        List<BucketInfluencer> bucketInfluencers = b.getBucketInfluencers();
        assertEquals(2, bucketInfluencers.size());
        assertEquals("bucketTime", bucketInfluencers.get(0).getInfluencerFieldName());
        assertEquals(0.03, bucketInfluencers.get(0).getProbability(), EPSILON);
        assertEquals(0.05, bucketInfluencers.get(0).getRawAnomalyScore(), EPSILON);
        assertEquals(95.4, bucketInfluencers.get(0).getInitialAnomalyScore(), EPSILON);
        assertEquals(95.4, bucketInfluencers.get(0).getAnomalyScore(), EPSILON);

        assertEquals("user", bucketInfluencers.get(1).getInfluencerFieldName());
        assertEquals(0.02, bucketInfluencers.get(1).getProbability(), EPSILON);
        assertEquals(0.13, bucketInfluencers.get(1).getRawAnomalyScore(), EPSILON);
        assertEquals(33.2, bucketInfluencers.get(1).getInitialAnomalyScore(), EPSILON);
        assertEquals(33.2, bucketInfluencers.get(1).getAnomalyScore(), EPSILON);

        List<Influencer> influencers = b.getInfluencers();
        assertEquals(2, influencers.size());

        Influencer inf = influencers.get(0);
        assertEquals("src_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.150", inf.getInfluencerFieldValue());
        assertEquals(0.9, inf.getProbability(), 0.0001);
        assertEquals(97.1948, inf.getInitialAnomalyScore(), 0.0001);

        inf = influencers.get(1);
        assertEquals(0.4, inf.getProbability(), 0.0001);
        assertEquals(12.1948, inf.getInitialAnomalyScore(), 0.0001);
        assertEquals("dst_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.1", inf.getInfluencerFieldValue());
    }

}
