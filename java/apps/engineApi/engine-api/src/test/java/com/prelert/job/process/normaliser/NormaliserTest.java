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
import java.util.Arrays;
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
import com.prelert.job.results.BucketInfluencer;

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
    public void testNormalise_GivenSingleBucketInfluencerAndRecords() throws NativeProcessRunException,
            UnknownJobException, IOException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(1.0);
        bucket1.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.001, 1.0));
        bucket1.setRecords(Arrays.asList(
                createSumButesRecord(0.05, 20.0, 1.0),
                createSumButesRecord(0.03, 30.0, 1.0)
                ));
        bucket1.setMaxNormalizedProbability(30.0);

        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(2.0);
        bucket2.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.002, 2.0));
        bucket2.setRecords(Arrays.asList(
                createSumButesRecord(0.03, 30.0, 2.0),
                createSumButesRecord(0.05, 0.2, 2.0)
                ));
        bucket2.setMaxNormalizedProbability(30.0);

        Bucket bucket3 = new Bucket();
        bucket3.setAnomalyScore(3.0);
        bucket3.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.003, 3.0));
        bucket3.setRecords(Arrays.asList(
                createSumButesRecord(0.01, 90.0, 3.0),
                createSumButesRecord(0.02, 80.0, 3.0)
                ));
        bucket3.setMaxNormalizedProbability(90.0);

        String normalisedResults = "{\"normalizedScore\":\"10.0\"}"  // Bucket 1
                + "{\"normalizedScore\":\"5.0\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"3.0\"}"                   // Bucket 1, Record 2
                + "{\"normalizedScore\":\"20.0\"}"                   // Bucket 2
                + "{\"normalizedScore\":\"3.0\"}"                   // Bucket 2, Record 1
                + "{\"normalizedScore\":\"0.41\"}"                   // Bucket 2, Record 2
                + "{\"normalizedScore\":\"30.0\"}"                   // Bucket 3
                + "{\"normalizedScore\":\"1.0\"}"                   // Bucket 3, Record 1
                + "{\"normalizedScore\":\"2.0\"}";                  // Bucket 3, Record 2

        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        List<Bucket> buckets = Arrays.asList(bucket1, bucket2, bucket3);

        m_Normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        InOrder inOrder = Mockito.inOrder(m_Process, writer);
        inOrder.verify(writer).writeRecord(
                new String[] {"level", "partitionFieldName", "partitionFieldValue", "personFieldName", "functionName", "valueFieldName", "probability"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "bucketTime", "", "", "0.001"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.05"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.03"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "bucketTime", "", "", "0.002"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.03"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.05"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "bucketTime", "", "", "0.003"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.01"});
        inOrder.verify(writer).writeRecord(new String[] {"leaf", "", "", "", "sum", "bytes", "0.02"});
        inOrder.verify(m_Process).closeOutputStream();

        assertTrue(bucket1.hadBigNormalisedUpdate());
        assertEquals(10.0, bucket1.getAnomalyScore(), ERROR);
        assertEquals(10.0, bucket1.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertTrue(bucket1.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(10.0, bucket1.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(5.0, bucket1.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket1.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(10.0, bucket1.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(3.0, bucket1.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(5.0, bucket1.getMaxNormalizedProbability(), ERROR);

        assertTrue(bucket2.hadBigNormalisedUpdate());
        assertEquals(20.0, bucket2.getAnomalyScore(), ERROR);
        assertEquals(20.0, bucket2.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertTrue(bucket2.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(20.0, bucket2.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(3.0, bucket2.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket2.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(20.0, bucket2.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(0.41, bucket2.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(3.0, bucket2.getMaxNormalizedProbability(), ERROR);

        assertTrue(bucket3.hadBigNormalisedUpdate());
        assertEquals(30.0, bucket3.getAnomalyScore(), ERROR);
        assertEquals(30.0, bucket3.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertTrue(bucket3.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(30.0, bucket3.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(1.0, bucket3.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket3.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(30.0, bucket3.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(2.0, bucket3.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(2.0, bucket3.getMaxNormalizedProbability(), ERROR);
    }

    @Test
    public void testNormalise_GivenMultipleBucketInfluencersWithBigUpdateAndRecordsNoBigUpdate() throws NativeProcessRunException,
            UnknownJobException, IOException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(20.0);
        bucket1.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.001, 10.0));
        bucket1.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.002, 20.0));
        bucket1.setRecords(Arrays.asList(
                createSumButesRecord(0.05, 5.0, 20.0),
                createSumButesRecord(0.03, 7.0, 20.0)
                ));

        String normalisedResults = "{\"normalizedScore\":\"80.0\"}"  // Bucket 1, BucketInfluencer 1
                + "{\"normalizedScore\":\"90.0\"}"                   // Bucket 1, BucketInfluencer 2
                + "{\"normalizedScore\":\"5.0\"}"                    // Bucket 1, record 1
                + "{\"normalizedScore\":\"7.0\"}";                   // Bucket 1, record 2

        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        List<Bucket> buckets = Arrays.asList(bucket1);

        m_Normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        InOrder inOrder = Mockito.inOrder(m_Process, writer);
        inOrder.verify(writer).writeRecord(
                new String[] {"level", "partitionFieldName", "partitionFieldValue", "personFieldName", "functionName", "valueFieldName", "probability"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "bucketTime", "", "", "0.001"});
        inOrder.verify(writer).writeRecord(new String[] {"root", "", "", "bucketTime", "", "", "0.002"});
        inOrder.verify(m_Process).closeOutputStream();

        assertTrue(bucket1.hadBigNormalisedUpdate());
        assertEquals(90.0, bucket1.getAnomalyScore(), ERROR);
        assertEquals(80.0, bucket1.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertEquals(90.0, bucket1.getBucketInfluencers().get(1).getAnomalyScore(), ERROR);
        assertTrue(bucket1.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(90.0, bucket1.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(5.0, bucket1.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket1.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(90.0, bucket1.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(7.0, bucket1.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(7.0, bucket1.getMaxNormalizedProbability(), ERROR);
    }

    @Test
    public void testNormalise_GivenNewAnomalyScoreIsNoBigUpdate() throws IOException,
            NativeProcessRunException, UnknownJobException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);
        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(30.0);
        bucket1.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.001, 30.0));
        bucket1.setRecords(Arrays.asList(
                createSumButesRecord(0.01, 0.1, 30.0),
                createSumButesRecord(0.02, 0.2, 30.0)
                ));
        bucket1.setMaxNormalizedProbability(0.2);

        Bucket bucket2 = new Bucket();
        bucket2.setAnomalyScore(0.1);
        bucket2.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.049, 0.1));
        bucket2.setRecords(Arrays.asList(
                createSumButesRecord(0.03, 0.3, 0.1),
                createSumButesRecord(0.04, 0.4, 0.1)
                ));
        bucket1.setMaxNormalizedProbability(0.4);

        String normalisedResults = "{\"normalizedScore\":\"30.9\"}"  // Bucket 1
                + "{\"normalizedScore\":\"0.11\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"0.19\"}"                   // Bucket 1, Record 2
                + "{\"normalizedScore\":\"0.05\"}"                   // Bucket 2
                + "{\"normalizedScore\":\"0.28\"}"                   // Bucket 2, Record 1
                + "{\"normalizedScore\":\"0.44\"}";                  // Bucket 2, Record 2
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        Normaliser normaliser = new Normaliser(JOB_ID, m_ProcessFactory, m_Logger);
        List<Bucket> buckets = Arrays.asList(bucket1, bucket2);

        normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        assertFalse(bucket1.hadBigNormalisedUpdate());
        assertEquals(30.0, bucket1.getAnomalyScore(), ERROR);
        assertEquals(30.0, bucket1.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertFalse(bucket1.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(30.0, bucket1.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(0.1, bucket1.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertFalse(bucket1.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(30.0, bucket1.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(0.2, bucket1.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(0.2, bucket1.getMaxNormalizedProbability(), ERROR);

        assertFalse(bucket2.hadBigNormalisedUpdate());
        assertEquals(0.1, bucket2.getAnomalyScore(), ERROR);
        assertEquals(0.1, bucket2.getBucketInfluencers().get(0).getAnomalyScore(), ERROR);
        assertFalse(bucket2.getRecords().get(0).hadBigNormalisedUpdate());
        assertEquals(0.1, bucket2.getRecords().get(0).getAnomalyScore(), ERROR);
        assertEquals(0.3, bucket2.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertFalse(bucket2.getRecords().get(1).hadBigNormalisedUpdate());
        assertEquals(0.1, bucket2.getRecords().get(1).getAnomalyScore(), ERROR);
        assertEquals(0.4, bucket2.getRecords().get(1).getNormalizedProbability(), ERROR);
        assertEquals(0.4, bucket2.getMaxNormalizedProbability(), ERROR);
    }

    @Test
    public void testNormalise_GivenMaxNormalizedProbabilityChanged() throws IOException,
            NativeProcessRunException, UnknownJobException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);
        when(m_ProcessFactory.create(JOB_ID, "quantilesState", 10, m_Logger)).thenReturn(m_Process);

        Bucket bucket1 = new Bucket();
        bucket1.setAnomalyScore(30.0);
        bucket1.addBucketInfluencer(createBucketInfluencer(BucketInfluencer.BUCKET_TIME, 0.001, 30.0));
        bucket1.setRecords(Arrays.asList(
                createSumButesRecord(0.01, 0.4, 30.0),
                createSumButesRecord(0.02, 0.2, 30.0)
                ));
        bucket1.setMaxNormalizedProbability(0.2);

        String normalisedResults = "{\"normalizedScore\":\"30.0\"}" // Bucket 1
                + "{\"normalizedScore\":\"0.1\"}"                   // Bucket 1, Record 1
                + "{\"normalizedScore\":\"0.2\"}";                  // Bucket 1, Record 2
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        List<Bucket> buckets = Arrays.asList(bucket1);

        Normaliser normaliser = new Normaliser(JOB_ID, m_ProcessFactory, m_Logger);

        normaliser.normalise(10, transformToNormalisable(buckets), "quantilesState");

        assertEquals(0.2, bucket1.getMaxNormalizedProbability(), ERROR);
        assertTrue(bucket1.hadBigNormalisedUpdate());
        assertEquals(0.1, bucket1.getRecords().get(0).getNormalizedProbability(), ERROR);
        assertTrue(bucket1.getRecords().get(0).hadBigNormalisedUpdate());
        assertFalse(bucket1.getRecords().get(1).hadBigNormalisedUpdate());
    }

    private static BucketInfluencer createBucketInfluencer(String field, double probability,
            double score)
    {
        BucketInfluencer bucketInfluencer = new BucketInfluencer();
        bucketInfluencer.setInfluencerFieldName(field);
        bucketInfluencer.setProbability(probability);
        bucketInfluencer.setAnomalyScore(score);
        return bucketInfluencer;
    }

    private static AnomalyRecord createSumButesRecord(double probability, double normProbability,
            double anomalyScore)
    {
        AnomalyRecord anomalyRecord = new AnomalyRecord();
        anomalyRecord.setAnomalyScore(anomalyScore);
        anomalyRecord.setProbability(probability);
        anomalyRecord.setNormalizedProbability(normProbability);
        anomalyRecord.setFunction(SUM);
        anomalyRecord.setFieldName(BYTES);
        return anomalyRecord;
    }

    private static List<Normalisable> transformToNormalisable(List<Bucket> buckets)
    {
        return buckets.stream().map(bucket -> new BucketNormalisable(bucket))
                .collect(Collectors.toList());
    }
}
