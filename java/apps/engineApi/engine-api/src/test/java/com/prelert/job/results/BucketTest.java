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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Detector;
import com.prelert.utils.json.AutoDetectParseException;

public class BucketTest
{
    @Test
    public void testEquals_GivenSameObject()
    {
        Bucket bucket = new Bucket();
        assertTrue(bucket.equals(bucket));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        Bucket bucket = new Bucket();
        assertFalse(bucket.equals("a string"));
    }

    @Test
    public void testEquals_GivenTwoDefaultBuckets()
    {
        Bucket bucket1 = new Bucket();
        Bucket bucket2 = new Bucket();

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentAnomalyScore()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(3.0);
        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(2.0);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentMaxNormalizedProbability()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setMaxNormalizedProbability(55.0);
        Bucket bucket2 = new Bucket();
        bucket2.setMaxNormalizedProbability(55.1);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentRawAnomalyScore()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRawAnomalyScore(0.03);
        Bucket bucket2 = new Bucket();
        bucket2.setRawAnomalyScore(0.3);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentEventCount()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setEventCount(3);
        Bucket bucket2 = new Bucket();
        bucket2.setEventCount(100);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentRecordCount()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecordCount(300);
        Bucket bucket2 = new Bucket();
        bucket2.setRecordCount(400);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentDetectors()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setDetectors(Arrays.asList(new Detector()));
        Bucket bucket2 = new Bucket();
        bucket2.setDetectors(null);

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenOneHasRecordsAndTheOtherDoesNot()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(new AnomalyRecord()));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(null);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentNumberOfRecords()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(new AnomalyRecord()));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(Arrays.asList(new AnomalyRecord(), new AnomalyRecord()));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenSameNumberOfRecordsButDifferent()
    {
        AnomalyRecord anomalyRecord1 = new AnomalyRecord();
        anomalyRecord1.setAnomalyScore(1.0);
        AnomalyRecord anomalyRecord2 = new AnomalyRecord();
        anomalyRecord1.setAnomalyScore(2.0);

        Bucket bucket1 = new Bucket();
        bucket1.setRecords(Arrays.asList(anomalyRecord1));
        Bucket bucket2 = new Bucket();
        bucket2.setRecords(Arrays.asList(anomalyRecord2));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentIsInterim()
    {
        Bucket bucket1 = new Bucket();
        bucket1.setInterim(true);
        Bucket bucket2 = new Bucket();
        bucket2.setInterim(false);

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenDifferentInfluences()
    {
        Bucket bucket1 = new Bucket();
        Influence influence = new Influence("testField");
        Bucket bucket2 = new Bucket();
        bucket2.setInfluences(Arrays.asList(influence));

        assertFalse(bucket1.equals(bucket2));
        assertFalse(bucket2.equals(bucket1));
    }

    @Test
    public void testEquals_GivenEqualBuckets()
    {
        Detector detector = new Detector();
        AnomalyRecord record = new AnomalyRecord();
        Influence influence = new Influence("testField");
        influence.addScore(new InfluenceScore("fieldValA", 1.0));
        influence.addScore(new InfluenceScore("fieldValB", 0.3));
        Date date = new Date();

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(42.0);
        bucket1.setDetectors(Arrays.asList(detector));
        bucket1.setEventCount(134);
        bucket1.setId("13546461");
        bucket1.setInterim(true);
        bucket1.setMaxNormalizedProbability(33.3);
        bucket1.setRawAnomalyScore(0.005);
        bucket1.setRecordCount(4);
        bucket1.setRecords(Arrays.asList(record));
        bucket1.setInfluences(Arrays.asList(influence));
        bucket1.setTimestamp(date);

        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(42.0);
        bucket2.setDetectors(Arrays.asList(detector));
        bucket2.setEventCount(134);
        bucket2.setId("13546461");
        bucket2.setInterim(true);
        bucket2.setMaxNormalizedProbability(33.3);
        bucket2.setRawAnomalyScore(0.005);
        bucket2.setRecordCount(4);
        bucket2.setRecords(Arrays.asList(record));
        bucket2.setInfluences(Arrays.asList(influence));
        bucket2.setTimestamp(date);

        assertTrue(bucket1.equals(bucket2));
        assertTrue(bucket2.equals(bucket1));
    }

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
                + "\"influences\" : {"
                    + "\"host\": [{\"web-server\": 0.8}]"
                  + "},"
                + "\"detectors\" : []"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        Bucket b = Bucket.parseJson(parser);
        assertEquals(1369437000000l, b.getTimestamp().getTime());
        assertEquals(2.0, b.getMaxNormalizedProbability(), 0.0001);
        assertEquals(50.0, b.getAnomalyScore(), 0.0001);
        assertEquals("1369437000", b.getId());
        assertEquals(5.0, b.getRawAnomalyScore(), 0.001);
        assertEquals(0, b.getRecordCount());
        assertEquals(0, b.getDetectors().size());
        assertEquals(1693, b.getEventCount());
        assertFalse(b.isInterim());

        List<Influence> influences = b.getInfluences();
        assertEquals(1, influences.size());

        Influence host = influences.get(0);
        assertEquals("host", host.getField());
        assertEquals(1, host.getScores().size());
        assertEquals("web-server", host.getScores().get(0).getFieldValue());
        assertEquals(0.8, host.getScores().get(0).getInfluence(), 0.001);
    }
}
