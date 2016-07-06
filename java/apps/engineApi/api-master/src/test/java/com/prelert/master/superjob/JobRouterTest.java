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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        OutputStream streamA = mock(OutputStream.class);
        OutputStream streamB = mock(OutputStream.class);
        OutputStream streamC = mock(OutputStream.class);

        List<OutputStream> streams = new ArrayList<>();
        streams.add(streamA);
        streams.add(streamB);
        streams.add(streamC);

        JobRouter router = new JobRouter(streams);

        String msg = "Hello Jobs";
        router.routeToAll(msg);

        byte [] output = msg.getBytes(StandardCharsets.UTF_8);
        verify(streamA, times(1)).write(output);
        verify(streamB, times(1)).write(output);
        verify(streamC, times(1)).write(output);
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
        byte [] output = msg.getBytes(StandardCharsets.UTF_8);

        // these strings will result in the values 0, 1 and 2 at least once
        for (String partition : new String [] {"table", "tat", "coat", "elephant", "aras"})
            router.routeToJob(partition, msg);

        verify(streamA, atLeastOnce()).write(output);
        verify(streamB, atLeastOnce()).write(output);
        verify(streamC, atLeastOnce()).write(output);
    }
}
