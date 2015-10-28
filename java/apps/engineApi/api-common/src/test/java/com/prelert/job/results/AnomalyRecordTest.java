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

import java.util.Arrays;
import java.util.Date;

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
        anomalyRecord.setPartitionFieldValue("AAL");

        anomalyRecord.setId("1403701200individual metric/0/0/responsetime/airline/AAL1");

        assertEquals("1403701200", anomalyRecord.getParent());
        assertEquals("1403701200individual metric/0/0/responsetime/airline/AAL1",
                anomalyRecord.getId());
    }

    @Test
    public void testGenerateNewId()
    {
        AnomalyRecord anomalyRecord = new AnomalyRecord();
        anomalyRecord.setId("1403701200individual metric/42");

        anomalyRecord.generateNewId("1403704800", "population count/", 48);

        assertEquals("1403704800population count/48", anomalyRecord.getId());
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

    @Test
    public void testEquals_GivenSameReference()
    {
        AnomalyRecord record = new AnomalyRecord();
        assertTrue(record.equals(record));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        AnomalyRecord record = new AnomalyRecord();
        assertFalse(record.equals("a string"));
    }

    @Test
    public void testEquals_GivenEqualRecords()
    {
        Influence influence = new Influence();

        AnomalyRecord record1 = new AnomalyRecord();
        record1.setActual(42.0);
        record1.setAnomalyScore(99.0);
        record1.setByFieldName("airline");
        record1.setByFieldValue("AAL");
        record1.setFieldName("responsetime");
        record1.setFunction("metric");
        record1.setFunctionDescription("Function blah blah");
        record1.setId("1403701200individual metric/42");
        record1.setInfluencers(Arrays.asList(influence));
        record1.setInterim(false);
        record1.setNormalizedProbability(86.4);
        record1.setOverFieldName("airport");
        record1.setOverFieldValue("SKG");
        record1.setPartitionFieldName("planet");
        record1.setPartitionFieldValue("earth");
        record1.setProbability(0.00042);
        record1.setTimestamp(new Date(0));
        record1.setTypical(0.5);

        AnomalyRecord record2 = new AnomalyRecord();
        record2.setActual(42.0);
        record2.setAnomalyScore(99.0);
        record2.setByFieldName("airline");
        record2.setByFieldValue("AAL");
        record2.setFieldName("responsetime");
        record2.setFunction("metric");
        record2.setFunctionDescription("Function blah blah");
        record2.setId("1403701200individual metric/42");
        record2.setInfluencers(Arrays.asList(influence));
        record2.setInterim(false);
        record2.setNormalizedProbability(86.4);
        record2.setOverFieldName("airport");
        record2.setOverFieldValue("SKG");
        record2.setPartitionFieldName("planet");
        record2.setPartitionFieldValue("earth");
        record2.setProbability(0.00042);
        record2.setTimestamp(new Date(0));
        record2.setTypical(0.5);

        assertTrue(record1.equals(record2));
        assertTrue(record2.equals(record1));
    }
}
