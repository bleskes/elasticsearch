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

package com.prelert.job.status;

import com.google.common.annotations.VisibleForTesting;

/**
 * Calculates the whole number of buckets between the last bucket
 * result and the latest data record. This is the bucket latency.
 * The latency is averaged over a number of samples.
 * Thread safe
 */
class ProcessingLatency
{
    private final long m_BucketSpan;

    // Circular buffer
    private final long [] m_SampleBuffer;
    private int m_SampleCount;
    private int m_CurrentIndex;

    public ProcessingLatency(long bucketSpan)
    {
        this(bucketSpan, 10);
    }

    /**
     *
     * @param bucketSpan
     * @param numberSamples Number of samples to average over
     */
    public ProcessingLatency(long bucketSpan, int numberSamples)
    {
        m_BucketSpan = bucketSpan;
        m_SampleBuffer = new long[numberSamples];
    }

    /**
     * Returns the actual buffer so not thread-safe
     * @return
     */
    @VisibleForTesting
    long [] getSamples()
    {
        return m_SampleBuffer;
    }

    public void addMeasure(long lastRecordTime, long lastBucketTime)
    {
        long numBucketsBetween = (lastRecordTime - lastBucketTime) / m_BucketSpan;
        addSample(numBucketsBetween);
    }

    synchronized void addSample(long sample)
    {
        if (m_SampleCount < m_SampleBuffer.length)
        {
            m_SampleCount++;
        }

        m_SampleBuffer[m_CurrentIndex] = sample;
        m_CurrentIndex = ++m_CurrentIndex % m_SampleBuffer.length;
    }

    int getCurrentIndex()
    {
        return m_CurrentIndex;
    }

    int getSampleCount()
    {
        return m_SampleCount;
    }

    /**
     * The average latency over the last N samples.
     *
     * @return 0 if no samples have been set else the bucket latency
     */
    synchronized public double latency()
    {
        if (m_SampleCount == 0)
        {
            return 0;
        }

        int sum = 0;
        for (int i=0; i<m_SampleCount; i++)
        {
            sum += m_SampleBuffer[i];
        }

        return sum / (double)m_SampleCount;
    }
}
