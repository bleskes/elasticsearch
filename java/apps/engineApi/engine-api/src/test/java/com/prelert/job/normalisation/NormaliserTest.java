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

package com.prelert.job.normalisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.UnknownJobException;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.quantiles.QuantilesState;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;

public class NormaliserTest
{
    private static final double ERROR = 0.01;

    @Mock private JobProvider m_JobProvider;
    @Mock private NormaliserProcessFactory m_ProcessFactory;
    @Mock private NormaliserProcess m_Process;
    @Mock private Logger m_Logger;

    private Normaliser m_Normaliser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNormaliseForSystemChange() throws NativeProcessRunException,
            UnknownJobException, IOException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);

        String normalisedResults =
                "{\"anomalyScore\":\"11.0\"}{\"anomalyScore\":\"12.0\"}{\"anomalyScore\":\"13.0\"}";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        when(m_ProcessFactory.create("foo", null, null, 10, m_Logger)).thenReturn(m_Process);
        when(m_JobProvider.getQuantilesState("foo")).thenReturn(new QuantilesState());

        double[] rawScoresPerBucket = {1.0, 2.0, 3.0};
        double[] probabilityPerRecord = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
        List<Bucket> buckets = createBuckets(rawScoresPerBucket, probabilityPerRecord);

        m_Normaliser = new Normaliser("foo", m_JobProvider, m_ProcessFactory, m_Logger);

        List<Bucket> normalisedBuckets = m_Normaliser.normaliseForSystemChange(10, buckets);

        assertEquals(buckets, normalisedBuckets);

        InOrder inOrder = Mockito.inOrder(m_Process, writer);
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("rawAnomalyScore");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("1.0");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("2.0");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("3.0");
        inOrder.verify(m_Process).closeOutputStream();

        for (int i = 0; i < buckets.size(); i++)
        {
            Bucket bucket = buckets.get(i);
            assertEquals(10 + rawScoresPerBucket[i], bucket.getAnomalyScore(), ERROR);
            for (AnomalyRecord record : bucket.getRecords())
            {
                assertEquals(10 + rawScoresPerBucket[i], record.getAnomalyScore(), ERROR);
            }
        }
    }

    @Test
    public void testNormaliseForUnusualBehaviour() throws NativeProcessRunException,
            UnknownJobException, IOException
    {
        LengthEncodedWriter writer = mock(LengthEncodedWriter.class);
        when(m_Process.createProcessWriter()).thenReturn(writer);

        String normalisedResults ="{\"normalizedProbability\":\"0.15\"}" +
                "{\"normalizedProbability\":\"0.13\"}" +
                "{\"normalizedProbability\":\"0.13\"}" +
                "{\"normalizedProbability\":\"0.15\"}" +
                "{\"normalizedProbability\":\"0.11\"}" +
                "{\"normalizedProbability\":\"0.11\"}";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(normalisedResults.getBytes());
        when(m_Process.createNormalisedResultsParser(m_Logger)).thenReturn(
                new NormalisedResultsParser(inputStream, m_Logger));

        when(m_ProcessFactory.create("foo", null, null, 10, m_Logger)).thenReturn(m_Process);
        when(m_JobProvider.getQuantilesState("foo")).thenReturn(new QuantilesState());

        double[] rawScoresPerBucket = {1.0, 2.0, 3.0};
        double[] probabilityPerRecord = {0.05, 0.03, 0.03, 0.05, 0.01, 0.01};
        List<Bucket> buckets = createBuckets(rawScoresPerBucket, probabilityPerRecord);

        m_Normaliser = new Normaliser("foo", m_JobProvider, m_ProcessFactory, m_Logger);

        List<Bucket> normalisedBuckets = m_Normaliser.normaliseForUnusualBehaviour(10, buckets);

        assertEquals(buckets, normalisedBuckets);

        InOrder inOrder = Mockito.inOrder(m_Process, writer);
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("probability");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.05");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.03");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.03");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.05");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.01");
        inOrder.verify(writer).writeNumFields(1);
        inOrder.verify(writer).writeField("0.01");
        inOrder.verify(m_Process).closeOutputStream();

        Bucket bucket1 = buckets.get(0);
        assertEquals(0.15, bucket1.getMaxNormalizedProbability(), ERROR);
        List<AnomalyRecord> bucket1Records = bucket1.getRecords();
        assertEquals(0.15, bucket1Records.get(0).getNormalizedProbability(), ERROR);
        assertEquals(0.13, bucket1Records.get(1).getNormalizedProbability(), ERROR);

        Bucket bucket2 = buckets.get(1);
        assertEquals(0.15, bucket2.getMaxNormalizedProbability(), ERROR);
        List<AnomalyRecord> bucket2Records = bucket2.getRecords();
        assertEquals(0.13, bucket2Records.get(0).getNormalizedProbability(), ERROR);
        assertEquals(0.15, bucket2Records.get(1).getNormalizedProbability(), ERROR);

        Bucket bucket3 = buckets.get(2);
        assertEquals(0.11, bucket3.getMaxNormalizedProbability(), ERROR);
        List<AnomalyRecord> bucket3Records = bucket3.getRecords();
        assertEquals(0.11, bucket3Records.get(0).getNormalizedProbability(), ERROR);
        assertEquals(0.11, bucket3Records.get(1).getNormalizedProbability(), ERROR);
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
            List<AnomalyRecord> records = new ArrayList<>();
            for (int recordIndex = 0; recordIndex < 2; recordIndex++)
            {
                int recordCount = bucketIndex * 2 + recordIndex;
                AnomalyRecord anomalyRecord = new AnomalyRecord();
                anomalyRecord.setAnomalyScore(anomalyScorePerBucket[bucketIndex]);
                anomalyRecord.setProbability(normalizedProbabilityPerRecord[recordCount]);
                records.add(anomalyRecord);
            }
            bucket.setRecords(records);
            buckets.add(bucket);
        }
        return buckets;
    }
}
