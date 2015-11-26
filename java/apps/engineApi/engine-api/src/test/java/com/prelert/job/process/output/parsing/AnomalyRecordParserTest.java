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
    private static final double ERROR = 0.001;


    @Test (expected = AutoDetectParseException.class)
    public void testParseJson_GivenParserDoesNotPointAtStartObject()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);

        AnomalyRecordParser.parseJson(parser);
    }

    @Test
    public void testParseJson_GivenEmptyInput()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        assertEquals(new AnomalyRecord(), AnomalyRecordParser.parseJson(parser));
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    @Test
    public void testParseJson_GivenAnomalyRecordWithAllFieldsPopulatedAndValid()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{\"probability\": 0.01,"
                + "\"anomalyScore\" : 42.0,"
                + "\"normalizedProbability\" : 0.05,"
                + "\"byFieldName\" : \"someByFieldName\","
                + "\"byFieldValue\" : \"someByFieldValue\","
                + "\"partitionFieldName\" : \"somePartitionFieldName\","
                + "\"partitionFieldValue\" : \"somePartitionFieldValue\","
                + "\"function\" : \"someFunction\","
                + "\"functionDescription\" : \"someFunctionDesc\","
                + "\"typical\" : 3.3,"
                + "\"actual\" : 1.3,"
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

        AnomalyRecord anomalyRecord = AnomalyRecordParser.parseJson(parser);

        assertEquals(0.01, anomalyRecord.getProbability(), ERROR);
        assertEquals(42.0, anomalyRecord.getAnomalyScore(), ERROR);
        assertEquals(0.05, anomalyRecord.getNormalizedProbability(), ERROR);
        assertEquals("someByFieldName", anomalyRecord.getByFieldName());
        assertEquals("someByFieldValue", anomalyRecord.getByFieldValue());
        assertEquals("somePartitionFieldName", anomalyRecord.getPartitionFieldName());
        assertEquals("somePartitionFieldValue", anomalyRecord.getPartitionFieldValue());
        assertEquals("someFunction", anomalyRecord.getFunction());
        assertEquals("someFunctionDesc", anomalyRecord.getFunctionDescription());
        assertEquals(3.3, anomalyRecord.getTypical(), ERROR);
        assertEquals(1.3, anomalyRecord.getActual(), ERROR);
        assertEquals("someFieldName", anomalyRecord.getFieldName());
        assertEquals("someOverFieldName", anomalyRecord.getOverFieldName());
        assertEquals("someOverFieldValue", anomalyRecord.getOverFieldValue());
        assertTrue(anomalyRecord.isInterim());
        assertEquals(2, anomalyRecord.getCauses().size());
        assertEquals(0.01, anomalyRecord.getCauses().get(0).getProbability(), ERROR);
        assertEquals(0.02, anomalyRecord.getCauses().get(1).getProbability(), ERROR);

        assertEquals(2, anomalyRecord.getInfluencers().size());
        assertEquals("host", anomalyRecord.getInfluencers().get(0).getInfluencerFieldName());
        assertEquals("user", anomalyRecord.getInfluencers().get(1).getInfluencerFieldName());


        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }


    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
