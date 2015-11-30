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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.ModelSizeStats;
import com.prelert.utils.json.AutoDetectParseException;

public class ModelSizeStatsParserTest
{
    @Test
    public void testParse() throws JsonParseException, IOException, AutoDetectParseException
    {
        String input = "{\"modelSizeStats\": 1,"
                + "\"totalByFieldCount\" : 2,"
                + "\"totalOverFieldCount\" : 3,"
                + "\"totalPartitionFieldCount\" : 4,"
                + "\"bucketAllocationFailuresCount\" : 5,"
                + "\"memoryStatus\" : \"OK\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ModelSizeStats stats = new ModelSizeStatsParser(parser).parseJson();

        assertEquals(1L, stats.getModelBytes());
        assertEquals(2L, stats.getTotalByFieldCount());
        assertEquals(3L, stats.getTotalOverFieldCount());
        assertEquals(4L, stats.getTotalPartitionFieldCount());
        assertEquals(5L, stats.getBucketAllocationFailuresCount());
        assertEquals("OK", stats.getMemoryStatus());
    }

    private static final JsonParser createJsonParser(String input) throws JsonParseException,
            IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
