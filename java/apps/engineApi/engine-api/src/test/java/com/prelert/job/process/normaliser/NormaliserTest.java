/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.process.normaliser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.UnknownJobException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.output.parsing.NormalisedResultsParser;
import com.prelert.job.process.writer.LengthEncodedWriter;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

public class NormaliserTest
{
    private static final double ERROR = 0.01;
    private static final String JOB_ID = "foo";
    private static final String SUM = "sum";
    private static final String BYTES = "bytes";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private NormaliserProcessFactory m_ProcessFactory;
    @Mock private NormaliserProcess m_Process;
    @Mock private Logger m_Logger;

    private Normaliser m_Normaliser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_Normaliser = new Normaliser(JOB_ID, m_ProcessFactory, m_Logger);
    }

    @Test
    public void testNormalise_GivenExceptionUponCreatingNormaliserProcess() throws IOException,
            NativeProcessRunException, UnknownJobException
    {
        doThrow(new IOException()).when(m_ProcessFactory).create(JOB_ID, "", 10, m_Logger);

        m_ExpectedException.expect(NativeProcessRunException.class);
        m_ExpectedException.expectMessage("Failed to start normalisation process for job foo");
        m_Normaliser.normalise(10, new ArrayList<>(), "");
    }

    @Test
    public void testNormalise() throws NativeProcessRunException,
            UnknownJobException, IOException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);

        double[] rawScoresPerBucket = {1.0, 2.0, 3.0};
        double[] probabilityPerRecord = {0.05, 0.03, 0.03, 0.05, 0.01, 0.02};

        // The normalised results are (10 + raw score) for buckets and (100 * prob) for records
        String normalisedResults = "{\"normalizedScore\":\"11.0\"}"  // Bucket 1
                + "{\"normalizedScore\":\"5.0\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"3.0\"}"                   // Bucket 1, Record 2
                + "{\"normalizedScore\":\"12.0\"}"                   // Bucket 2
                + "{\"normalizedScore\":\"3.0\"}"                   // Bucket 2, Record 1
                + "{\"normalizedScore\":\"5.0\"}"                   // Bucket 2, Record 2
                + "{\"normalizedScore\":\"13.0\"}"                   // Bucket 3
                + "{\"normalizedScore\":\"1.0\"}"                   // Bucket 3, Record 1
                + "{\"normalizedScore\":\"2.0\"}";                  // Bucket 3, Record 2

        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        List<Bucket> buckets = createBuckets(rawScoresPerBucket, probabilityPerRecord);

        m_Normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        InOrder inOrder = Mockito.inOrder(m_Process, writer);
        inOrder.verify(writer).writeRecord(
                new String[] {"level", "partitionFieldName", "personFieldName", "functionName", "valueFieldName", "rawScore"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "", "", "1.0"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.05"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.03"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "", "", "2.0"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.03"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.05"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "", "", "3.0"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.01"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "sum", "bytes", "0.02"});
        inOrder.verify(m_Process).closeOutputStream();

        for (int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++)
        {
            Bucket bucket = buckets.get(bucketIndex);
            assertEquals(10 + rawScoresPerBucket[bucketIndex], bucket.getAnomalyScore(), ERROR);
            assertTrue(bucket.hadBigNormalisedUpdate());

            for (int i = 0; i < bucket.getRecords().size(); i++)
            {
                AnomalyRecord record = bucket.getRecords().get(i);
                int recordIndex = (bucketIndex * 2) + i;

                assertEquals(10 + rawScoresPerBucket[bucketIndex], record.getAnomalyScore(), ERROR);
                assertEquals(100 * probabilityPerRecord[recordIndex], record.getNormalizedProbability(), ERROR);
                assertTrue(record.hadBigNormalisedUpdate());
            }
        }
    }

    @Test
    public void testNormalise_GivenNewAnomalyScoreIsNoBigUpdate() throws IOException,
            NativeProcessRunException, UnknownJobException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);
        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        String normalisedResults = "{\"normalizedScore\":\"30.9\"}"  // Bucket 1
                + "{\"normalizedScore\":\"0.11\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"0.19\"}"                   // Bucket 1, Record 2
                + "{\"normalizedScore\":\"0.05\"}"                   // Bucket 2
                + "{\"normalizedScore\":\"0.33\"}"                   // Bucket 2, Record 1
                + "{\"normalizedScore\":\"0.44\"}";                  // Bucket 2, Record 2
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        double[] rawScoresPerBucket = {30.0, 0.1};
        double[] probabilityPerRecord = {0.1, 0.2, 0.3, 0.4};
        List<Bucket> buckets = createBuckets(rawScoresPerBucket, probabilityPerRecord);

        Normaliser normaliser = new Normaliser(JOB_ID, m_ProcessFactory, m_Logger);

        normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        for (int i = 0; i < buckets.size(); i++)
        {
            Bucket bucket = buckets.get(i);
            assertEquals(rawScoresPerBucket[i], bucket.getAnomalyScore(), ERROR);
            assertFalse(bucket.hadBigNormalisedUpdate());
            for (AnomalyRecord record : bucket.getRecords())
            {
                assertFalse(record.hadBigNormalisedUpdate());
            }
        }
    }

    @Test
    public void testNormalise_GivenMaxNormalizedProbabilityChanged() throws IOException,
            NativeProcessRunException, UnknownJobException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);
        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        String normalisedResults = "{\"normalizedScore\":\"30.0\"}" // Bucket 1
                + "{\"normalizedScore\":\"0.4\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"0.2\"}";                  // Bucket 1, Record 2
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        double[] rawScoresPerBucket = {30.0};
        double[] probabilityPerRecord = {0.1, 0.2};
        List<Bucket> buckets = createBuckets(rawScoresPerBucket, probabilityPerRecord);

        Normaliser normaliser = new Normaliser(JOB_ID, m_ProcessFactory, m_Logger);

        normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        Bucket bucket = buckets.get(0);
        assertEquals(0.4, bucket.getMaxNormalizedProbability(), ERROR);
        assertTrue(bucket.hadBigNormalisedUpdate());
        assertEquals(0.4, bucket.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket.getRecords().get(0).hadBigNormalisedUpdate());
        assertFalse(bucket.getRecords().get(1).hadBigNormalisedUpdate());
    }

    private static List<Bucket> createBuckets(double[] anomalyScorePerBucket,
            double[] normalizedProbabilityPerRecord)
    {
        assertTrue(normalizedProbabilityPerRecord.length == 2 * anomalyScorePerBucket.length);
        List<Bucket> buckets = new ArrayList<>();
        for (int bucketIndex = 0; bucketIndex < anomalyScorePerBucket.length; bucketIndex++)
        {
            Bucket bucket = new Bucket();
            bucket.setRawAnomalyScore(anomalyScorePerBucket[bucketIndex]);
            bucket.setAnomalyScore(anomalyScorePerBucket[bucketIndex]);
            List<AnomalyRecord> records = new ArrayList<>();
            for (int recordIndex = 0; recordIndex < 2; recordIndex++)
            {
                int recordCount = bucketIndex * 2 + recordIndex;
                AnomalyRecord anomalyRecord = new AnomalyRecord();
                anomalyRecord.setAnomalyScore(anomalyScorePerBucket[bucketIndex]);
                anomalyRecord.setProbability(normalizedProbabilityPerRecord[recordCount]);
                anomalyRecord.setNormalizedProbability(normalizedProbabilityPerRecord[recordCount]);
                anomalyRecord.setFunction(SUM);
                anomalyRecord.setFieldName(BYTES);
                records.add(anomalyRecord);
            }
            bucket.setRecords(records);
            buckets.add(bucket);
        }
        return buckets;
    }

    private static List<Normalisable> transformToNormalisable(List<Bucket> buckets)
    {
        return buckets.stream().map(bucket -> new BucketNormalisable(bucket))
                .collect(Collectors.toList());
    }
}
