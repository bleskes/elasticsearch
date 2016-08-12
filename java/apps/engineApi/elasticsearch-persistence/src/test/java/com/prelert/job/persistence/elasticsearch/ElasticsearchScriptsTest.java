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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.script.Script;
import org.junit.Test;

public class ElasticsearchScriptsTest
{
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
}
