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

package com.prelert.job;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class ModelSizeStatsTest
{
    @Test
    public void testDefaultConstructor()
    {
        ModelSizeStats stats = new ModelSizeStats();
        assertEquals("modelSizeStats", stats.getModelSizeStatsId());
        assertEquals(0, stats.getModelBytes());
        assertEquals(0, stats.getTotalByFieldCount());
        assertEquals(0, stats.getTotalOverFieldCount());
        assertEquals(0, stats.getTotalPartitionFieldCount());
        assertEquals(0, stats.getBucketAllocationFailuresCount());
        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetModelSizeStatsId()
    {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setModelSizeStatsId("foo");

        assertEquals("foo", stats.getModelSizeStatsId());
    }

    @Test
    public void testSetMemoryStatus_GivenNull()
    {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus(null);

        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetMemoryStatus_GivenEmpty()
    {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus("");

        assertEquals("OK", stats.getMemoryStatus());
    }

    @Test
    public void testSetMemoryStatus_GivenSoftLimit()
    {
        ModelSizeStats stats = new ModelSizeStats();

        stats.setMemoryStatus("SOFT_LIMIT");

        assertEquals("SOFT_LIMIT", stats.getMemoryStatus());
    }

    @Test
    public void testSerialise() throws IOException
    {
        ModelSizeStats stats = new ModelSizeStats();
        stats.setTimestamp(new Date(1234L));
        stats.setModelBytes(101L);
        stats.setTotalByFieldCount(201L);
        stats.setTotalOverFieldCount(301L);
        stats.setTotalPartitionFieldCount(401L);
        stats.setBucketAllocationFailuresCount(501L);
        stats.setMemoryStatus("OK");
        stats.setLogTime(new Date(6789L));

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        stats.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"modelBytes\":101,"
                + "\"totalByFieldCount\":201,"
                + "\"totalPartitionFieldCount\":401,"
                + "\"bucketAllocationFailuresCount\":501,"
                + "\"totalOverFieldCount\":301,"
                + "\"@timestamp\":1234,"
                + "\"memoryStatus\":\"OK\","
                + "\"logTime\":6789"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
