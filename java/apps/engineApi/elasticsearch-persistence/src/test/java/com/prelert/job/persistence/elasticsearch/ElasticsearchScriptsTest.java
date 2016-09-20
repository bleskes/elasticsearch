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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;

public class ElasticsearchScriptsTest
{
    @Captor private ArgumentCaptor<Map<String, Object>> m_MapCaptor;

    @Before
    public void setUp() throws InterruptedException, ExecutionException
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNewUpdateBucketCount()
    {
        Script script = ElasticsearchScripts.newUpdateBucketCount(42L);
        assertEquals("ctx._source.counts.bucketCount += params.count", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(42L, script.getParams().get("count"));
    }

    @Test
    public void testNewUpdateUsage()
    {
        Script script = ElasticsearchScripts.newUpdateUsage(1L, 2L, 3L);
        assertEquals("ctx._source.inputBytes += params.bytes;ctx._source.inputFieldCount += params.fieldCount;ctx._source.inputRecordCount += params.recordCount;", script.getScript());
        assertEquals(3, script.getParams().size());
        assertEquals(1L, script.getParams().get("bytes"));
        assertEquals(2L, script.getParams().get("fieldCount"));
        assertEquals(3L, script.getParams().get("recordCount"));
    }

    @Test
    public void testNewUpdateCategorizationFilters()
    {
        List<String> newFilters = Arrays.asList("foo", "bar");

        Script script = ElasticsearchScripts.newUpdateCategorizationFilters(newFilters);

        assertEquals("ctx._source.analysisConfig.categorizationFilters = params.newFilters;", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(newFilters, script.getParams().get("newFilters"));
    }

    @Test
    public void testNewUpdateDetectorDescription()
    {
        Script script = ElasticsearchScripts.newUpdateDetectorDescription(2, "Almost Blue");
        assertEquals("ctx._source.analysisConfig.detectors[params.detectorIndex].detectorDescription = params.newDescription;", script.getScript());
        assertEquals(2, script.getParams().size());
        assertEquals(2, script.getParams().get("detectorIndex"));
        assertEquals("Almost Blue", script.getParams().get("newDescription"));
    }

    @Test
    public void testNewUpdateDetectorRules()
    {
        List<Map<String, Object>> newRules = new ArrayList<>();
        Script script = ElasticsearchScripts.newUpdateDetectorRules(1, newRules);
        assertEquals("ctx._source.analysisConfig.detectors[params.detectorIndex].detectorRules = params.newDetectorRules;", script.getScript());
        assertEquals(2, script.getParams().size());
        assertEquals(1, script.getParams().get("detectorIndex"));
        assertEquals(newRules, script.getParams().get("newDetectorRules"));
    }

    @Test
    public void testNewUpdateSchedulerConfig()
    {
        Map<String, Object> newSchedulerConfig = new HashMap<>();
        newSchedulerConfig.put("foo", "bar");

        Script script = ElasticsearchScripts.newUpdateSchedulerConfig(newSchedulerConfig);

        assertEquals("ctx._source.schedulerConfig = params.newSchedulerConfig;", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(newSchedulerConfig, script.getParams().get("newSchedulerConfig"));
    }

    @Test
    public void testUpdateProcessingTime()
    {
        Long time = 135790L;
        Script script = ElasticsearchScripts.updateProcessingTime(time);
        assertEquals("ctx._source.averageProcessingTimeMs = ctx._source.averageProcessingTimeMs * 0.9 + params.timeMs * 0.1", script.getScript());
        assertEquals(time, script.getParams().get("timeMs"));
    }

    @Test
    public void testUpdateUpsertViaScript() throws JobException
    {
        String index = "idx";
        String docId = "docId";
        String type = "type";
        Map<String, Object> map = new HashMap<>();
        map.put("testKey",  "testValue");

        Script script = new Script("test-script-here", ScriptType.INLINE, null, map);
        ArgumentCaptor<Script> captor = ArgumentCaptor.forClass(Script.class);

        MockClientBuilder clientBuilder = new MockClientBuilder("cluster")
                .prepareUpdateScript(index, type, docId, captor, m_MapCaptor);
        Client client = clientBuilder.build();

        assertTrue(ElasticsearchScripts.updateViaScript(client, index, type, docId, script));

        Script response = captor.getValue();
        assertEquals(script, response);
        assertEquals(map, response.getParams());

        map.clear();
        map.put("secondKey",  "secondValue");
        map.put("thirdKey",  "thirdValue");
        assertTrue(ElasticsearchScripts.upsertViaScript(client, index, type, docId, script, map));

        Map<String, Object> updatedParams = m_MapCaptor.getValue();
        assertEquals(map, updatedParams);
    }

    @Test
    public void testUpdateUpsertViaScript_InvalidIndex() throws JobException
    {
        String index = "idx";
        String docId = "docId";
        String type = "type";

        IndexNotFoundException e = new IndexNotFoundException("INF");

        Script script = new Script("foo");
        ArgumentCaptor<Script> captor = ArgumentCaptor.forClass(Script.class);

        MockClientBuilder clientBuilder = new MockClientBuilder("cluster")
                .prepareUpdateScript(index, type, docId, captor, m_MapCaptor, e);
        Client client = clientBuilder.build();

        try
        {
            ElasticsearchScripts.updateViaScript(client, index, type, docId, script);
            assertFalse(true);
        }
        catch (UnknownJobException ex)
        {
            assertEquals(index, ex.getJobId());
        }
    }

    @Test
    public void testUpdateUpsertViaScript_IllegalArgument() throws JobException
    {
        String index = "idx";
        String docId = "docId";
        String type = "type";
        Map<String, Object> map = new HashMap<>();
        map.put("testKey",  "testValue");

        IllegalArgumentException ex = new IllegalArgumentException("IAE");

        Script script = new Script("test-script-here", ScriptType.INLINE, null, map);
        ArgumentCaptor<Script> captor = ArgumentCaptor.forClass(Script.class);

        MockClientBuilder clientBuilder = new MockClientBuilder("cluster")
                .prepareUpdateScript(index, type, docId, captor, m_MapCaptor, ex);
        Client client = clientBuilder.build();

        try
        {
            ElasticsearchScripts.updateViaScript(client, index, type, docId, script);
        }
        catch (JobException e)
        {
            String msg = e.toString();
            assertTrue(msg.matches(".*test-script-here.*inline.*params.*testKey.*testValue.*"));
        }
    }

}
