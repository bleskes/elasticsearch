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

public class ModelSnapshotTest
{
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
        Date now = new Date();

        ModelSizeStats modelSizeStats = new ModelSizeStats();

        ModelSnapshot modelSnapshot1 = new ModelSnapshot();
        modelSnapshot1.setTimestamp(now);
        modelSnapshot1.setDescription("a snapshot");
        modelSnapshot1.setRestorePriority(1234L);
        modelSnapshot1.setSnapshotId("my_id");
        modelSnapshot1.setSnapshotDocCount(7);
        modelSnapshot1.setModelSizeStats(modelSizeStats);
        modelSnapshot1.setLatestRecordTimeStamp(new Date(12345678901234L));
        modelSnapshot1.setLatestResultTimeStamp(new Date(14345678901234L));

        ModelSnapshot modelSnapshot2 = new ModelSnapshot();
        modelSnapshot2.setTimestamp(now);
        modelSnapshot2.setDescription("a snapshot");
        modelSnapshot2.setRestorePriority(1234L);
        modelSnapshot2.setSnapshotId("my_id");
        modelSnapshot2.setSnapshotDocCount(7);
        modelSnapshot2.setModelSizeStats(modelSizeStats);
        modelSnapshot2.setLatestRecordTimeStamp(new Date(12345678901234L));
        modelSnapshot2.setLatestResultTimeStamp(new Date(14345678901234L));

        assertTrue(modelSnapshot1.equals(modelSnapshot2));
        assertTrue(modelSnapshot2.equals(modelSnapshot1));
        assertEquals(modelSnapshot1.hashCode(), modelSnapshot2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
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

        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setTimestamp(new Date(12345L));
        modelSnapshot.setDescription("a snapshot");
        modelSnapshot.setRestorePriority(1234L);
        modelSnapshot.setSnapshotId("my_id");
        modelSnapshot.setSnapshotDocCount(7);
        modelSnapshot.setModelSizeStats(modelSizeStats);
        modelSnapshot.setLatestRecordTimeStamp(new Date(12345678901234L));
        modelSnapshot.setLatestResultTimeStamp(new Date(14345678901234L));

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        modelSnapshot.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
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
}
