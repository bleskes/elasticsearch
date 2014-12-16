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

package com.prelert.job.usage.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.prelert.job.usage.Usage;

public class ElasticsearchUsageReporterTest
{

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX");

    @SuppressWarnings("rawtypes")
    @Test
    public void testPersistUsageCounts() throws ParseException
    {
        Client client = mock(Client.class);
        Logger logger = mock(Logger.class);
        final UpdateRequestBuilder updateRequestBuilder = createSelfReturningUpdateRequester();
        when(client.prepareUpdate(anyString(), anyString(), anyString())).thenReturn(
                updateRequestBuilder);
        ElasticsearchUsageReporter usageReporter = new ElasticsearchUsageReporter(client, "job1",
                logger);
        usageReporter.addBytesRead(10);
        usageReporter.addFieldsRecordsRead(30);

        usageReporter.persistUsageCounts();

        ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> upsertsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(client, times(2)).prepareUpdate(indexCaptor.capture(), eq(Usage.TYPE),
                idCaptor.capture());
        verify(updateRequestBuilder, times(2)).setScript("update-usage");
        verify(updateRequestBuilder, times(2)).addScriptParam("bytes", 10L);
        verify(updateRequestBuilder, times(2)).addScriptParam("fieldCount", 30L);
        verify(updateRequestBuilder, times(2)).addScriptParam("recordCount", 1L);
        verify(updateRequestBuilder, times(2)).setUpsert(upsertsCaptor.capture());
        verify(updateRequestBuilder, times(2)).setRetryOnConflict(3);
        verify(updateRequestBuilder, times(2)).get();

        assertEquals(Arrays.asList("prelert-usage", "job1"), indexCaptor.getAllValues());
        assertEquals(2, idCaptor.getAllValues().size());
        String id = idCaptor.getValue();
        assertEquals(id, idCaptor.getAllValues().get(0));
        assertTrue(id.startsWith("usage-"));
        String timestamp = id.substring("usage-".length());

        assertEquals("prelert-usage", indexCaptor.getAllValues().get(0));
        assertEquals("job1", indexCaptor.getAllValues().get(1));

        List<Map> capturedUpserts = upsertsCaptor.getAllValues();
        assertEquals(2, capturedUpserts.size());
        assertEquals(DATE_FORMAT.parse(timestamp), capturedUpserts.get(0).get(Usage.TIMESTAMP));
        assertEquals(10L, capturedUpserts.get(0).get(Usage.INPUT_BYTES));
        assertEquals(30L, capturedUpserts.get(0).get(Usage.INPUT_FIELD_COUNT));
        assertEquals(1L, capturedUpserts.get(0).get(Usage.INPUT_RECORD_COUNT));
    }

    private UpdateRequestBuilder createSelfReturningUpdateRequester()
    {
        return mock(UpdateRequestBuilder.class, new Answer<Object>(){
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getMethod().getReturnType() == UpdateRequestBuilder.class)
                {
                    return invocation.getMock();
                }
                return null;
            }
        });
    }
}
