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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.prelert.job.JobException;

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
        assertEquals("update-bucket-count", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(42L, script.getParams().get("count"));
    }

    @Test
    public void testNewUpdateUsage()
    {
        Script script = ElasticsearchScripts.newUpdateUsage(1L, 2L, 3L);
        assertEquals("update-usage", script.getScript());
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

        assertEquals("update-categorization-filters", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(newFilters, script.getParams().get("newFilters"));
    }

    @Test
    public void testNewUpdateDetectorDescription()
    {
        Script script = ElasticsearchScripts.newUpdateDetectorDescription(2, "Almost Blue");
        assertEquals("update-detector-description", script.getScript());
        assertEquals(2, script.getParams().size());
        assertEquals(2, script.getParams().get("detectorIndex"));
        assertEquals("Almost Blue", script.getParams().get("newDescription"));
    }

    @Test
    public void testNewUpdateDetectorRules()
    {
        List<Map<String, Object>> newRules = new ArrayList<>();
        Script script = ElasticsearchScripts.newUpdateDetectorRules(1, newRules);
        assertEquals("update-detector-rules", script.getScript());
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

        assertEquals("update-scheduler-config", script.getScript());
        assertEquals(1, script.getParams().size());
        assertEquals(newSchedulerConfig, script.getParams().get("newSchedulerConfig"));
    }

    @Test
    public void testUpdateProcessingTime()
    {
        Long time = 135790L;
        Script script = ElasticsearchScripts.updateProcessingTime(time);
        System.out.println(script.getScript());
        assertEquals("update-average-processing-time", script.getScript());
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
}
