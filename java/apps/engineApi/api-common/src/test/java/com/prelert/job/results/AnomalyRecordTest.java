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

package com.prelert.job.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class AnomalyRecordTest
{
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
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();

        assertTrue(record1.equals(record2));
        assertTrue(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentActual()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setActual(new double[] { record1.getActual()[0] + 1.0 });

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentAnomalyScore()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setAnomalyScore(record1.getAnomalyScore() + 1.0);

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentByFieldName()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setByFieldName(record1.getByFieldName() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentCorrelatedByFieldValue()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setCorrelatedByFieldValue(record1.getCorrelatedByFieldValue() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentByFieldValue()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setByFieldValue(record1.getByFieldValue() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentFieldName()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setFieldName(record1.getFieldName() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentFunction()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setFunction(record1.getFunction() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentFunctionDescription()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setFunctionDescription(record1.getFunctionDescription() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentId()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setId(record1.getId() + "0");

        assertTrue(record1.equals(record2));
        assertTrue(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentInfluencers()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setInfluencers(Collections.emptyList());

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentInterim()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setInterim(!record1.isInterim());

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentInitialNormalizedProbability()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setInitialNormalizedProbability(record1.getNormalizedProbability() + 0.1);

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentNormalizedProbability()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setNormalizedProbability(record1.getNormalizedProbability() + 0.1);

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentOverFieldName()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setOverFieldName(record1.getOverFieldName() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentOverFieldValue()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setOverFieldValue(record1.getOverFieldValue() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentPartitionFieldName()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setPartitionFieldName(record1.getPartitionFieldName() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentPartitionFieldValue()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setPartitionFieldValue(record1.getPartitionFieldValue() + ".diff");

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentProbability()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setProbability(record1.getProbability() + 0.001);

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentTimestamp()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        Date timestamp = record1.getTimestamp();
        Date newTimestamp = new Date(timestamp.getTime() + 1000);
        record1.setTimestamp(newTimestamp);

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testEquals_GivenDifferentTypical()
    {
        AnomalyRecord record1 = createFullyPopulatedRecord();
        AnomalyRecord record2 = createFullyPopulatedRecord();
        record1.setTypical(new double[] { record1.getTypical()[0] + 42.0 });

        assertFalse(record1.equals(record2));
        assertFalse(record2.equals(record1));
    }

    @Test
    public void testSerialise() throws IOException
    {
        AnomalyRecord record = createFullyPopulatedRecord();
        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();

        serialiser.startObject();
        record.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"actual\":42.0,"
                + "\"bucketSpan\":3600,"
                + "\"overFieldValue\":\"SKG\","
                + "\"airport.reversed\":\"SKG\","
                + "\"fieldName\":\"responsetime\","
                + "\"partitionFieldValue\":\"earth\","
                + "\"airline.reversed\":\"AAL\","
                + "\"initialNormalizedProbability\":90.2,"
                + "\"probability\":4.2E-4,"
                + "\"byFieldValue\":\"AAL\","
                + "\"overFieldName\":\"airport\","
                + "\"planet.reversed\":\"earth\","
                + "\"partitionFieldName\":\"planet\","
                + "\"anomalyScore\":99.0,"
                + "\"@timestamp\":0,"
                + "\"normalizedProbability\":86.4,"
                + "\"byFieldName\":\"airline\","
                + "\"correlatedByFieldValue\":\"UAL\","
                + "\"function\":\"metric\","
                + "\"typical\":0.5,"
                + "\"influencers\":["
                +   "{"
                +     "\"influencerFieldName\":\"airline\","
                +     "\"influencerFieldValues\":[\"AAL\"]"
                +   "}"
                + "],"
                + "\"functionDescription\":\"Function blah blah\","
                + "\"detectorIndex\":0"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    private static AnomalyRecord createFullyPopulatedRecord()
    {
        AnomalyRecord record = new AnomalyRecord();
        record.setActual(new double[] { 42.0 });
        record.setBucketSpan(3600);
        record.setAnomalyScore(99.0);
        record.setByFieldName("airline");
        record.setByFieldValue("AAL");
        record.setCorrelatedByFieldValue("UAL");
        record.setFieldName("responsetime");
        record.setFunction("metric");
        record.setFunctionDescription("Function blah blah");
        Influence influence = new Influence();
        influence.setInfluencerFieldName("airline");
        influence.setInfluencerFieldValues(Arrays.asList("AAL"));
        record.setInfluencers(Arrays.asList(influence));
        record.setInterim(false);
        record.setNormalizedProbability(86.4);
        record.setInitialNormalizedProbability(90.2);
        record.setOverFieldName("airport");
        record.setOverFieldValue("SKG");
        record.setPartitionFieldName("planet");
        record.setPartitionFieldValue("earth");
        record.setProbability(0.00042);
        record.setTimestamp(new Date(0));
        record.setTypical(new double[] { 0.5 });
        record.setId("1403701200individual metric/planet/earth42");
        return record;
    }
}
