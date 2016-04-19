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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.ModelSizeStats;

public class ModelSizeStatsParserTest
{
    @Test
    public void testParse() throws IOException
    {
        String input = "{\"modelBytes\": 1,"
                + "\"totalByFieldCount\" : 2,"
                + "\"totalOverFieldCount\" : 3,"
                + "\"totalPartitionFieldCount\" : 4,"
                + "\"bucketAllocationFailuresCount\" : 5,"
                + "\"memoryStatus\" : \"OK\","
                + "\"bucketTime\" : 1444333321"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        Date d1 = new Date();
        ModelSizeStats stats = new ModelSizeStatsParser(parser).parseJson();
        Date d2 = new Date();

        assertEquals(1L, stats.getModelBytes());
        assertEquals(2L, stats.getTotalByFieldCount());
        assertEquals(3L, stats.getTotalOverFieldCount());
        assertEquals(4L, stats.getTotalPartitionFieldCount());
        assertEquals(5L, stats.getBucketAllocationFailuresCount());
        assertEquals(1444333321000L, stats.getTimestamp().getTime());
        assertTrue(stats.getLogTime().getTime() >= d1.getTime());
        assertTrue(stats.getLogTime().getTime() <= d2.getTime());
        assertEquals("OK", stats.getMemoryStatus());
    }

    private static final JsonParser createJsonParser(String input) throws IOException
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
