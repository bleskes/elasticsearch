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

package com.prelert.job.persistence.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.prelert.job.JobException;
import com.prelert.job.usage.Usage;

public class ElasticsearchUsagePersisterTest
{
    @SuppressWarnings("rawtypes")
    @Test
    public void testPersistUsageCounts() throws ParseException, JobException
    {
        Client client = mock(Client.class);
        Logger logger = mock(Logger.class);
        final UpdateRequestBuilder updateRequestBuilder = createSelfReturningUpdateRequester();

        when(client.prepareUpdate(anyString(), anyString(), anyString())).thenReturn(
                updateRequestBuilder);

        ElasticsearchUsagePersister persister = new ElasticsearchUsagePersister(client, logger);

        persister.persistUsage("job1", 10l, 30l, 1l);

        ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> upsertsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Script> updateScriptCaptor = ArgumentCaptor.forClass(Script.class);

        verify(client, times(2)).prepareUpdate(indexCaptor.capture(), eq(Usage.TYPE),
                idCaptor.capture());
        verify(updateRequestBuilder, times(2)).setScript(updateScriptCaptor.capture());
        verify(updateRequestBuilder, times(2)).setUpsert(upsertsCaptor.capture());
        verify(updateRequestBuilder, times(2)).setRetryOnConflict(ElasticsearchScripts.UPDATE_JOB_RETRY_COUNT);
        verify(updateRequestBuilder, times(2)).get();

        assertEquals(Arrays.asList("prelert-usage", "prelertresults-job1"), indexCaptor.getAllValues());
        assertEquals(2, idCaptor.getAllValues().size());
        String id = idCaptor.getValue();
        assertEquals(id, idCaptor.getAllValues().get(0));
        assertTrue(id.startsWith("usage-"));
        String timestamp = id.substring("usage-".length());

        assertEquals("prelert-usage", indexCaptor.getAllValues().get(0));
        assertEquals("prelertresults-job1", indexCaptor.getAllValues().get(1));

        Script script = updateScriptCaptor.getValue();
        assertEquals("update-usage", script.getScript());
        assertEquals(ScriptService.ScriptType.FILE, script.getType());
        assertEquals("groovy", script.getLang());
        Map<String, Object> scriptParams = script.getParams();
        assertEquals(3, scriptParams.size());
        assertEquals(10L, scriptParams.get("bytes"));
        assertEquals(30L, scriptParams.get("fieldCount"));
        assertEquals(1L, scriptParams.get("recordCount"));

        List<Map> capturedUpserts = upsertsCaptor.getAllValues();
        assertEquals(2, capturedUpserts.size());
        System.out.println(capturedUpserts.get(0).get(ElasticsearchMappings.ES_TIMESTAMP));
        assertEquals(timestamp, capturedUpserts.get(0).get(ElasticsearchMappings.ES_TIMESTAMP).toString());
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
