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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

public class AnomalyRecordTest
{
    @Test
    public void testGetId_GivenIdIsZero()
    {
        AnomalyRecord anomalyRecord = new AnomalyRecord();
        anomalyRecord.setId("0");

        assertNull(anomalyRecord.getId());
    }

    @Test
    public void testSetId_GivenNoPartitionField()
    {
        AnomalyRecord anomalyRecord = new AnomalyRecord();

        anomalyRecord.setId("1403701200individual metric/1");

        assertEquals("1403701200", anomalyRecord.getParent());
        assertEquals("1403701200individual metric/1", anomalyRecord.getId());
    }

    @Test
    public void testSetId_GivenPartitionField()
    {
        AnomalyRecord anomalyRecord = new AnomalyRecord();
        anomalyRecord.setFieldName("responsetime");
        anomalyRecord.setPartitionFieldName("airline");

        anomalyRecord.setId("1403701200individual metric/0/0/responsetime/airline/1");

        assertEquals("1403701200", anomalyRecord.getParent());
        assertEquals("1403701200individual metric/0/0/responsetime/airline/1",
                anomalyRecord.getId());
    }

    @Test
    public void testResetBigNormalisedUpdateFlag()
    {
        AnomalyRecord record = new AnomalyRecord();
        record.raiseBigNormalisedUpdateFlag();
        assertTrue(record.hadBigNormalisedUpdate());

        record.resetBigNormalisedUpdateFlag();

        assertFalse(record.hadBigNormalisedUpdate());
    }

    @Test
    public void testRaiseBigNormalisedUpdateFlag()
    {
        AnomalyRecord record = new AnomalyRecord();
        assertFalse(record.hadBigNormalisedUpdate());

        record.raiseBigNormalisedUpdateFlag();

        assertTrue(record.hadBigNormalisedUpdate());
    }

}
