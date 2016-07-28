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

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DataForkerTest
{
    private final static String HEADER = "timestamp,airline,responsetime";
    private final static String DATA = "1,BA,10\n2,KLM,4\n2,BA,5";

    @Test
    public void testWrite_RoutesHeaderToAll() throws IOException
    {
        JobRouter router = mock(JobRouter.class);

        DataForker forker = new DataForker("airline", router);

        ByteArrayInputStream stream = new ByteArrayInputStream(
                                            HEADER.getBytes(StandardCharsets.UTF_8));

        forker.forkData(stream);

        verify(router, times(1)).routeToAll(HEADER + '\n');
    }

    @Test
    public void testWrite() throws IOException
    {
        JobRouter router = mock(JobRouter.class);

        DataForker forker = new DataForker("airline", router);

        StringBuilder sb = new StringBuilder(HEADER).append("\n").append(DATA);

        ByteArrayInputStream stream = new ByteArrayInputStream(
                                        sb.toString().getBytes(StandardCharsets.UTF_8));


        Map<String, List<String>> writtenRecords = new HashMap<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                String key = (String) invocation.getArguments()[0];
                String record = (String) invocation.getArguments()[1];

                writtenRecords.computeIfAbsent(key, (s) -> new ArrayList<String>());
                writtenRecords.computeIfPresent(key, (k,v) -> {v.add(record); return v;});

                return null;
            }
        }).when(router).routeToJob(anyString(), anyString());

        forker.forkData(stream);

        assertTrue(writtenRecords.containsKey("KLM"));
        assertEquals(1, writtenRecords.get("KLM").size());
        assertEquals("2,KLM,4\n", writtenRecords.get("KLM").get(0));

        assertTrue(writtenRecords.containsKey("BA"));
        assertEquals(2, writtenRecords.get("BA").size());
        assertEquals("1,BA,10\n", writtenRecords.get("BA").get(0));
        assertEquals("2,BA,5\n", writtenRecords.get("BA").get(1));
    }
}
