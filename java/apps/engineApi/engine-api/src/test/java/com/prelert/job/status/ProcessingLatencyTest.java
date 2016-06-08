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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class ProcessingLatencyTest
{
    @Test
    public void testAddSample_CircularBufferRolls()
    {
        int SIZE = 5;
        ProcessingLatency pl = new ProcessingLatency(300, SIZE);
        for (int i=0; i<SIZE; i++)
        {
            assertEquals(i, pl.getCurrentIndex());
            pl.addSample(5);
            assertEquals(i+1, pl.getSampleCount());
        }

        assertEquals(0, pl.getCurrentIndex());
        pl.addSample(6);
        assertEquals(1, pl.getCurrentIndex());
        assertEquals(SIZE, pl.getSampleCount());
    }

    @Test
    public void testAverageLatency()
    {
        int SIZE = 8;
        ProcessingLatency pl = new ProcessingLatency(300, SIZE);

        Random rand = new Random();
        List<Integer> samples = new ArrayList<>();

        // fill the buffer
        for (int i=0; i<SIZE; i++)
        {
            int s = rand.nextInt(64);
            samples.add(s);
            pl.addSample(s);

            double mean = samples.stream().reduce(0, Integer::sum) / (double)(i +1);
            assertEquals(mean, pl.latency(), 0.000001);
        }

        // now overwriting elements in circular buffer
        int s = rand.nextInt(64);
        samples.set(0, s);
        pl.addSample(s);
        double mean = samples.stream().reduce(0, Integer::sum) / (double)(SIZE);
        assertEquals(mean, pl.latency(), 0.000001);

        s = rand.nextInt(64);
        samples.set(1, s);
        pl.addSample(s);
        mean = samples.stream().reduce(0, Integer::sum) / (double)(SIZE);
        assertEquals(mean, pl.latency(), 0.000001);
    }

    @Test
    public void testAddMeasure()
    {
        ProcessingLatency pl = new ProcessingLatency(300, 9);
        pl.addMeasure(910, 300);
        assertEquals(2, pl.getSamples()[0]);

        pl.addMeasure(890, 300);
        assertEquals(1, pl.getSamples()[1]);

        pl.addMeasure(301, 300);
        assertEquals(0, pl.getSamples()[2]);
    }

    @Test
    public void testLatencyEqualsZeroWhenNoDataSet()
    {
        ProcessingLatency pl = new ProcessingLatency(300, 9);
        assertEquals(0.0, pl.latency(), 0.000001);
    }

}
