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
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import com.prelert.job.JsonViews;
import com.prelert.job.ModelSizeStats;

public class ElasticsearchJobDetailsMapperTest
{
    @Mock private Client m_Client;
    private ObjectMapper m_ObjectMapper;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ObjectMapper = new ObjectMapper();
        m_ObjectMapper.setConfig(m_ObjectMapper.getSerializationConfig().withView(
                JsonViews.DatastoreView.class));
    }

    @Test (expected = CannotMapJobFromJson.class)
    public void testMap_GivenJobSourceCannotBeParsed()
    {
        Map<String, Object> source = new HashMap<>();
        source.put("invalidKey", true);

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(false);
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(getResponse);
        when(m_Client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE, ModelSizeStats.TYPE)).thenReturn(getRequestBuilder);

        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(m_Client, m_ObjectMapper);

        mapper.map(source);
    }

    @Test
    public void testMap_GivenModelSizeStatsExists()
    {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        modelSizeStats.setModelBytes(42L);
        Date now = new Date();
        modelSizeStats.setTimestamp(now);

        JobDetails originalJob = new JobDetails();
        originalJob.setId("foo");

        Map<String, Object> source = m_ObjectMapper.convertValue(originalJob,
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> modelSizeStatsSource = m_ObjectMapper.convertValue(modelSizeStats,
                new TypeReference<Map<String, Object>>() {});
        Object timestamp = modelSizeStatsSource.remove(ModelSizeStats.TIMESTAMP);
        modelSizeStatsSource.put(ElasticsearchMappings.ES_TIMESTAMP, timestamp);

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(true);
        when(getResponse.getSource()).thenReturn(modelSizeStatsSource);
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(getResponse);
        when(m_Client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE, ModelSizeStats.TYPE)).thenReturn(getRequestBuilder);

        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(m_Client, m_ObjectMapper);

        JobDetails mappedJob = mapper.map(source);

        assertEquals("foo", mappedJob.getId());
        assertEquals(42L, mappedJob.getModelSizeStats().getModelBytes());
        assertEquals(now, mappedJob.getModelSizeStats().getTimestamp());
    }

    @Test
    public void testMap_GivenModelSizeStatsDoesNotExist()
    {
        JobDetails originalJob = new JobDetails();
        originalJob.setId("foo");

        Map<String, Object> source = m_ObjectMapper.convertValue(originalJob,
                new TypeReference<Map<String, Object>>() {});

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(false);
        GetRequestBuilder getRequestBuilder = mock(GetRequestBuilder.class);
        when(getRequestBuilder.get()).thenReturn(getResponse);
        when(m_Client.prepareGet("prelertresults-foo", ModelSizeStats.TYPE, ModelSizeStats.TYPE)).thenReturn(getRequestBuilder);

        ElasticsearchJobDetailsMapper mapper = new ElasticsearchJobDetailsMapper(m_Client, m_ObjectMapper);

        JobDetails mappedJob = mapper.map(source);

        assertEquals("foo", mappedJob.getId());
        assertNull(mappedJob.getModelSizeStats());
    }
}
