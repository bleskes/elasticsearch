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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.utils.json.AutoDetectParseException;

public class AnomalyRecordParserTest
{
    private static final double EPSILON = 0.000001;


    @Test (expected = AutoDetectParseException.class)
    public void testParseJson_GivenParserDoesNotPointAtStartObject()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);

        new AnomalyRecordParser(parser).parseJson();
    }

    @Test
    public void testParseJson_GivenEmptyInput() throws IOException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        assertEquals(new AnomalyRecord(), new AnomalyRecordParser(parser).parseJson());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    @Test
    public void testParseJson_GivenAnomalyRecordWithAllFieldsPopulatedAndValid()
            throws IOException
    {
        String input = "{\"detectorIndex\": 3,"
                + "\"probability\" : 0.01,"
                + "\"anomalyScore\" : 42.0,"
                + "\"normalizedProbability\" : 0.05,"
                + "\"byFieldName\" : \"someByFieldName\","
                + "\"byFieldValue\" : \"someByFieldValue\","
                + "\"partitionFieldName\" : \"somePartitionFieldName\","
                + "\"partitionFieldValue\" : \"somePartitionFieldValue\","
                + "\"function\" : \"someFunction\","
                + "\"functionDescription\" : \"someFunctionDesc\","
                + "\"typical\" : [ 3.3 ],"
                + "\"actual\" : [ 1.3 ],"
                + "\"fieldName\" : \"someFieldName\","
                + "\"overFieldName\" : \"someOverFieldName\","
                + "\"overFieldValue\" : \"someOverFieldValue\","
                + "\"causes\" : [{\"probability\" : 0.01}, {\"probability\" : 0.02}],"
                + "\"influencers\" : {"
                    + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                    + "\"user\": [{\"cat\": 1}, {\"dave\": 0.4},{\"jo\": 0.1}]"
                    + "},"
                + "\"isInterim\" : true"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        AnomalyRecord anomalyRecord = new AnomalyRecordParser(parser).parseJson();

        assertEquals(3, anomalyRecord.getDetectorIndex());
        assertEquals(0.01, anomalyRecord.getProbability(), EPSILON);
        assertEquals(42.0, anomalyRecord.getAnomalyScore(), EPSILON);
        assertEquals(0.05, anomalyRecord.getNormalizedProbability(), EPSILON);
        assertEquals(0.05, anomalyRecord.getInitialNormalizedProbability(), EPSILON);
        assertEquals("someByFieldName", anomalyRecord.getByFieldName());
        assertEquals("someByFieldValue", anomalyRecord.getByFieldValue());
        assertEquals("somePartitionFieldName", anomalyRecord.getPartitionFieldName());
        assertEquals("somePartitionFieldValue", anomalyRecord.getPartitionFieldValue());
        assertEquals("someFunction", anomalyRecord.getFunction());
        assertEquals("someFunctionDesc", anomalyRecord.getFunctionDescription());
        assertEquals(3.3, anomalyRecord.getTypical()[0], EPSILON);
        assertEquals(1.3, anomalyRecord.getActual()[0], EPSILON);
        assertEquals("someFieldName", anomalyRecord.getFieldName());
        assertEquals("someOverFieldName", anomalyRecord.getOverFieldName());
        assertEquals("someOverFieldValue", anomalyRecord.getOverFieldValue());
        assertTrue(anomalyRecord.isInterim());
        assertEquals(2, anomalyRecord.getCauses().size());
        assertEquals(0.01, anomalyRecord.getCauses().get(0).getProbability(), EPSILON);
        assertEquals(0.02, anomalyRecord.getCauses().get(1).getProbability(), EPSILON);

        assertEquals(2, anomalyRecord.getInfluencers().size());
        assertEquals("host", anomalyRecord.getInfluencers().get(0).getInfluencerFieldName());
        assertEquals("user", anomalyRecord.getInfluencers().get(1).getInfluencerFieldName());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    @Test
    public void testParseJson_GivenMultivariateAnomalyRecordWithAllFieldsPopulatedAndValid()
            throws IOException
    {
        String input = "{\"detectorIndex\": 3,"
                + "\"probability\" : 0.01,"
                + "\"anomalyScore\" : 42.0,"
                + "\"normalizedProbability\" : 0.05,"
                + "\"byFieldName\" : \"someByFieldName\","
                + "\"byFieldValue\" : \"someByFieldValue\","
                + "\"partitionFieldName\" : \"somePartitionFieldName\","
                + "\"partitionFieldValue\" : \"somePartitionFieldValue\","
                + "\"function\" : \"lat_long\","
                + "\"functionDescription\" : \"lat_long\","
                + "\"typical\" : [ 3.3, 75 ],"
                + "\"actual\" : [ -13, 7.34 ],"
                + "\"fieldName\" : \"someFieldName\","
                + "\"influencers\" : {"
                    + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                    + "\"user\": [{\"cat\": 1}, {\"dave\": 0.4},{\"jo\": 0.1}]"
                    + "}"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        AnomalyRecord anomalyRecord = new AnomalyRecordParser(parser).parseJson();

        assertEquals(3, anomalyRecord.getDetectorIndex());
        assertEquals(0.01, anomalyRecord.getProbability(), EPSILON);
        assertEquals(42.0, anomalyRecord.getAnomalyScore(), EPSILON);
        assertEquals(0.05, anomalyRecord.getNormalizedProbability(), EPSILON);
        assertEquals(0.05, anomalyRecord.getInitialNormalizedProbability(), EPSILON);
        assertEquals("someByFieldName", anomalyRecord.getByFieldName());
        assertEquals("someByFieldValue", anomalyRecord.getByFieldValue());
        assertEquals("somePartitionFieldName", anomalyRecord.getPartitionFieldName());
        assertEquals("somePartitionFieldValue", anomalyRecord.getPartitionFieldValue());
        assertEquals("lat_long", anomalyRecord.getFunction());
        assertEquals("lat_long", anomalyRecord.getFunctionDescription());
        assertEquals(2, anomalyRecord.getTypical().length);
        assertEquals(3.3, anomalyRecord.getTypical()[0], EPSILON);
        assertEquals(75, anomalyRecord.getTypical()[1], EPSILON);
        assertEquals(2, anomalyRecord.getActual().length);
        assertEquals(-13, anomalyRecord.getActual()[0], EPSILON);
        assertEquals(7.34, anomalyRecord.getActual()[1], EPSILON);
        assertEquals("someFieldName", anomalyRecord.getFieldName());
        assertNull(anomalyRecord.getOverFieldName());
        assertNull(anomalyRecord.getOverFieldValue());
        assertFalse(anomalyRecord.isInterim());
        assertNull(anomalyRecord.getCauses());

        assertEquals(2, anomalyRecord.getInfluencers().size());
        assertEquals("host", anomalyRecord.getInfluencers().get(0).getInfluencerFieldName());
        assertEquals("user", anomalyRecord.getInfluencers().get(1).getInfluencerFieldName());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
