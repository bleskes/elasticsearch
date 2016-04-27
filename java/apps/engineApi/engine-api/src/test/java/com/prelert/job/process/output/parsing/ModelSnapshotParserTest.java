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
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.utils.json.AutoDetectParseException;

public class ModelSnapshotParserTest
{
    @Test (expected = AutoDetectParseException.class)
    public void testParseJson_GivenInvalidJson() throws IOException
    {
        String input = "\"snapshotId\": \"123\" }";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        new ModelSnapshotParser(parser).parseJson();
    }

    @Test
    public void testParseJson_GivenModelSnapshotWithAllFieldsPopulatedAndValid()
            throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{\"snapshotId\": \"123\","
                     + " \"description\":\"Very interesting\","
                     + " \"restorePriority\":123,"
                     + " \"timestamp\":1234567890000,"
                     + " \"snapshotDocCount\":3,"
                     + " \"modelSizeStats\":{\"modelBytes\":54321},"
                     + " \"quantiles\": {\"quantileState\": \"yabadabadoo\"},"
                     + " \"latestRecordTimeStamp\": 1111111111111,"
                     + " \"latestResultTimeStamp\" : 1010101010101}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ModelSnapshot modelSnapshot = new ModelSnapshotParser(parser).parseJson();

        assertEquals(new Date(1234567890000L), modelSnapshot.getTimestamp());
        assertEquals("123", modelSnapshot.getSnapshotId());
        assertEquals("Very interesting", modelSnapshot.getDescription());
        assertEquals(123L, modelSnapshot.getRestorePriority());
        assertEquals(3, modelSnapshot.getSnapshotDocCount());
        assertNotNull(modelSnapshot.getModelSizeStats());
        assertEquals(54321L, modelSnapshot.getModelSizeStats().getModelBytes());
        assertEquals(new Date(1111111111111L), modelSnapshot.getLatestRecordTimeStamp());
        assertEquals(new Date(1010101010101L), modelSnapshot.getLatestResultTimeStamp());
        Quantiles q = new Quantiles();
        q.setQuantileState("yabadabadoo");
        assertEquals(q, modelSnapshot.getQuantiles());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
