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

package com.prelert.data.extractor.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ScrollStateTest
{
    @Test
    public void testPeekAndMatchInStream() throws IOException
    {
        String initialResponse = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        ScrollState scrollState = ScrollState.createDefault(328);

        InputStream stream = scrollState.updateFromStream(toStream(initialResponse));

        assertEquals("c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1", scrollState.getScrollId());
        assertEquals(initialResponse, HttpGetResponse.getStreamAsString(stream));
    }

    @Test
    public void testPeekAndMatchInStream_GivenStreamCannotBeReadAtOnce() throws IOException
    {
        String response = "{"
                + "\"_scroll_id\":\"c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1\","
                + "\"took\":17,"
                + "\"timed_out\":false,"
                + "\"_shards\":{"
                + "  \"total\":1,"
                + "  \"successful\":1,"
                + "  \"failed\":0"
                + "},"
                + "\"hits\":{"
                + "  \"total\":1437,"
                + "  \"max_score\":null,"
                + "  \"hits\":["
                + "    \"_index\":\"dataIndex\","
                + "    \"_type\":\"dataType\","
                + "    \"_id\":\"1403481600\","
                + "    \"_score\":null,"
                + "    \"_source\":{"
                + "      \"id\":\"1403481600\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        // Imitate a stream that does not get fully read during the first read invocation
        PushbackInputStream stream = mock(PushbackInputStream.class);
        AtomicInteger invocationCount = new AtomicInteger(0);
        when(stream.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>()
        {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                byte[] buffer = (byte[]) invocation.getArguments()[0];
                int offset = (int) invocation.getArguments()[1];
                int length = (int) invocation.getArguments()[2];
                byte[] responseBytes = response.getBytes();
                invocationCount.incrementAndGet();
                if (invocationCount.get() == 1 && offset == 0 && length == 32768)
                {
                    System.arraycopy(responseBytes, 0, buffer, 0, 20);
                    return 20;
                }
                else if (invocationCount.get() == 2  && offset == 20 && length == 32748)
                {
                    System.arraycopy(responseBytes, 20, buffer, 20, responseBytes.length - 20);
                    return responseBytes.length - 20;
                }
                else if (invocationCount.get() == 3  && offset == responseBytes.length
                        && length == (32768 - responseBytes.length))
                {
                    return -1;
                }
                else
                {
                    throw new RuntimeException("Unexpected invocation");
                }
            }
        });

        ScrollState scrollState = ScrollState.createDefault(32768);

        Matcher matcher = scrollState.peekAndMatchInStream(stream, ScrollState.SCROLL_ID_PATTERN);

        assertTrue(matcher.find());
        assertEquals("c2Nhbjs2OzM0NDg1ODpzRlBLc0FXNlNyNm5JWUc1", matcher.group(1));
    }

    private static InputStream toStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}
