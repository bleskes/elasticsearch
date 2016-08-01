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

package com.prelert.master.superjob;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class JobRouterTest
{
    @Test
    public void testRouteToAll() throws IOException
    {
        ByteArrayOutputStream streamA = new ByteArrayOutputStream();
        ByteArrayOutputStream streamB = new ByteArrayOutputStream();
        ByteArrayOutputStream streamC = new ByteArrayOutputStream();

        List<OutputStream> streams = new ArrayList<>();
        streams.add(streamA);
        streams.add(streamB);
        streams.add(streamC);

        JobRouter router = new JobRouter(streams);

        String msg = "Hello Jobs";
        router.routeToAll(msg);

        router.close();

        byte [] output = msg.getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(output, streamA.toByteArray());
        assertArrayEquals(output, streamB.toByteArray());
        assertArrayEquals(output, streamC.toByteArray());
    }


    @Test
    public void testRouteToJob() throws IOException
    {
        OutputStream streamA = mock(OutputStream.class);
        OutputStream streamB = mock(OutputStream.class);
        OutputStream streamC = mock(OutputStream.class);

        List<OutputStream> streams = new ArrayList<>();
        streams.add(streamA);
        streams.add(streamB);
        streams.add(streamC);

        JobRouter router = new JobRouter(streams);

        String msg = "Hello Jobs";

        // these strings will result in the values 0, 1 and 2 at least once
        for (String partition : new String [] {"table", "tat", "coat", "elephant", "aras"})
            router.routeToJob(partition, msg);

        router.flush();
        router.close();

        verify(streamA, atLeastOnce()).write(any(byte[].class), eq(0), anyInt());
        verify(streamB, atLeastOnce()).write(any(byte[].class), eq(0), anyInt());
        verify(streamC, atLeastOnce()).write(any(byte[].class), eq(0), anyInt());
    }


    @Test
    public void test_multipleWritesToAll() throws IOException
    {
        ByteArrayOutputStream streamA = new ByteArrayOutputStream();
        ByteArrayOutputStream streamB = new ByteArrayOutputStream();

        List<OutputStream> streams = new ArrayList<>();
        streams.add(streamA);
        streams.add(streamB);

        JobRouter router = new JobRouter(streams, 8);

        router.routeToAll("Hello");
        router.routeToAll(" World");
        router.routeToAll(" Again");


        router.close();

        byte [] output = "Hello World Again".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(output, streamA.toByteArray());
        assertArrayEquals(output, streamB.toByteArray());
    }

    @Test
    public void test_multipleWrites_withSmallBuffer() throws IOException
    {
        ByteArrayOutputStream streamA = new ByteArrayOutputStream();

        List<OutputStream> streams = new ArrayList<>();
        streams.add(streamA);

        JobRouter router = new JobRouter(streams, 6);

        router.routeToAll("Hello");
        router.routeToJob("GGG", " World");
        router.routeToJob("GGG", " Again");


        router.close();

        byte [] output = "Hello World Again".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(output, streamA.toByteArray());
    }
}
