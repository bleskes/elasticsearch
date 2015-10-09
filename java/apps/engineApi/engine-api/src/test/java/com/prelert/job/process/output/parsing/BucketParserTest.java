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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;
import com.prelert.utils.json.AutoDetectParseException;

public class BucketParserTest
{
    @Test
    public void testParseJson() throws JsonParseException, IOException, AutoDetectParseException
    {
        String json = "{"
                + "\"timestamp\" : 1369437000,"
                + "\"maxNormalizedProbability\" : 2.0,"
                + "\"anomalyScore\" : 50.0,"
                + "\"id\" : \"1369437000\","
                + "\"rawAnomalyScore\" : 5.0,"
                + "\"eventCount\" : 1693,"
                + "\"isInterim\" : false,"
                + "\"influencers\" : ["
                    + "{\"probability\":0.9,\"initialAnomalyScore\":97.1948,\"influencerFieldName\":\"src_ip\",\"influencerFieldValue\":\"23.28.243.150\"},"
                    + "{\"probability\":0.4,\"initialAnomalyScore\":12.1948,\"influencerFieldName\":\"dst_ip\",\"influencerFieldValue\":\"23.28.243.1\"}"
                  + "],"
                + "\"detectors\" : []"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        Bucket b = BucketParser.parseJson(parser);
        assertEquals(1369437000000l, b.getTimestamp().getTime());
        assertEquals(2.0, b.getMaxNormalizedProbability(), 0.0001);
        assertEquals(50.0, b.getAnomalyScore(), 0.0001);
        assertEquals("1369437000", b.getId());
        assertEquals(5.0, b.getRawAnomalyScore(), 0.001);
        assertEquals(0, b.getRecordCount());
        assertEquals(0, b.getDetectors().size());
        assertEquals(1693, b.getEventCount());
        assertFalse(b.isInterim());

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
