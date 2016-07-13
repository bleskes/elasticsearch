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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;
import com.prelert.job.quantiles.Quantiles;

public class ModelSnapshotTest
{
    private static final Date DEFAULT_TIMESTAMP = new Date();
    private static final String DEFAULT_DESCRIPTION = "a snapshot";
    private static final String DEFAULT_ID = "my_id";
    private static final long DEFAULT_PRIORITY = 1234L;
    private static final int DEFAULT_DOC_COUNT = 7;
    private static final Date DEFAULT_LATEST_RESULT_TIMESTAMP = new Date(12345678901234L);
    private static final Date DEFAULT_LATEST_RECORD_TIMESTAMP = new Date(12345678904321L);

    @Test
    public void testEquals_GivenSameObject()
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();

        assertTrue(modelSnapshot.equals(modelSnapshot));
    }

    @Test
    public void testEquals_GivenObjectOfDifferentClass()
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();

        assertFalse(modelSnapshot.equals("a string"));
    }

    @Test
    public void testEquals_GivenEqualModelSnapshots()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();

        assertTrue(modelSnapshot1.equals(modelSnapshot2));
        assertTrue(modelSnapshot2.equals(modelSnapshot1));
        assertEquals(modelSnapshot1.hashCode(), modelSnapshot2.hashCode());
    }

    @Test
    public void testEquals_GivenDifferentTimestamp()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setTimestamp(new Date(modelSnapshot2.getTimestamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentDescription()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setDescription(modelSnapshot2.getDescription() + " blah");

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentRestorePriority()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setRestorePriority(modelSnapshot2.getRestorePriority() + 1);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentId()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setSnapshotId(modelSnapshot2.getSnapshotId() + "_2");

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentDocCount()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setSnapshotDocCount(modelSnapshot2.getSnapshotDocCount() + 1);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentModelSizeStats()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.getModelSizeStats().setModelBytes(42L);

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentQuantiles()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.getQuantiles().setQuantileState("different state");

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentLatestResultTimestamp()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setLatestResultTimeStamp(
                new Date(modelSnapshot2.getLatestResultTimeStamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testEquals_GivenDifferentLatestRecordTimestamp()
    {
        ModelSnapshot modelSnapshot1 = createFullyPopulated();
        ModelSnapshot modelSnapshot2 = createFullyPopulated();
        modelSnapshot2.setLatestRecordTimeStamp(
                new Date(modelSnapshot2.getLatestRecordTimeStamp().getTime() + 1));

        assertFalse(modelSnapshot1.equals(modelSnapshot2));
        assertFalse(modelSnapshot2.equals(modelSnapshot1));
    }

    @Test
    public void testSerialise_GivenFullyPopulated() throws IOException
    {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        modelSizeStats.setTimestamp(new Date(9123L));
        modelSizeStats.setModelBytes(1000L);
        modelSizeStats.setBucketAllocationFailuresCount(1L);
        modelSizeStats.setMemoryStatus("SOFT_LIMIT");
        modelSizeStats.setTotalByFieldCount(3L);
        modelSizeStats.setTotalOverFieldCount(4L);
        modelSizeStats.setTotalPartitionFieldCount(5L);
        modelSizeStats.setLogTime(new Date(42L));

        Quantiles quantiles = new Quantiles();
        quantiles.setQuantileState("my_q_state");
        quantiles.setTimestamp(new Date(43L));

        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(new Date(12345L));
        modelSnapshot.setDescription("a snapshot");
        modelSnapshot.setRestorePriority(1234L);
        modelSnapshot.setSnapshotId("my_id");
        modelSnapshot.setSnapshotDocCount(7);
        modelSnapshot.setModelSizeStats(modelSizeStats);
        modelSnapshot.setQuantiles(quantiles);
        modelSnapshot.setLatestRecordTimeStamp(new Date(12345678901234L));
        modelSnapshot.setLatestResultTimeStamp(new Date(14345678901234L));

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        modelSnapshot.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"quantiles\":{\"@timestamp\":43,\"quantileState\":\"my_q_state\"},"
                + "\"latestRecordTimeStamp\":12345678901234,"
                + "\"latestResultTimeStamp\":14345678901234,"
                + "\"@timestamp\":12345,"
                + "\"restorePriority\":1234,"
                + "\"snapshotId\":\"my_id\","
                + "\"description\":\"a snapshot\","
                + "\"snapshotDocCount\":7,"
                + "\"modelSizeStats\":{"
                +   "\"modelBytes\":1000,"
                +   "\"totalByFieldCount\":3,"
                +   "\"totalPartitionFieldCount\":5,"
                +   "\"bucketAllocationFailuresCount\":1,"
                +   "\"totalOverFieldCount\":4,"
                +   "\"@timestamp\":9123,"
                +   "\"memoryStatus\":\"SOFT_LIMIT\","
                +   "\"logTime\":42"
                + "}"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    @Test
    public void testSerialise_GivenMissingOptionals() throws IOException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(new Date(54321L));
        modelSnapshot.setDescription("another snapshot");
        modelSnapshot.setRestorePriority(4321L);
        modelSnapshot.setSnapshotId("another_id");
        modelSnapshot.setSnapshotDocCount(3);

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        modelSnapshot.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"@timestamp\":54321,"
                + "\"restorePriority\":4321,"
                + "\"snapshotId\":\"another_id\","
                + "\"description\":\"another snapshot\","
                + "\"snapshotDocCount\":3"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    private static ModelSnapshot createFullyPopulated()
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(DEFAULT_TIMESTAMP);
        modelSnapshot.setDescription(DEFAULT_DESCRIPTION);
        modelSnapshot.setRestorePriority(DEFAULT_PRIORITY);
        modelSnapshot.setSnapshotId(DEFAULT_ID);
        modelSnapshot.setSnapshotDocCount(DEFAULT_DOC_COUNT);
        modelSnapshot.setModelSizeStats(new ModelSizeStats());
        modelSnapshot.setLatestResultTimeStamp(DEFAULT_LATEST_RESULT_TIMESTAMP);
        modelSnapshot.setLatestRecordTimeStamp(DEFAULT_LATEST_RECORD_TIMESTAMP);
        modelSnapshot.setQuantiles(new Quantiles());
        return modelSnapshot;
    }
}
