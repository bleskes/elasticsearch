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

package com.prelert.rs.data.extraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.prelert.data.extractor.elasticsearch.ElasticsearchDataExtractor;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.password.PasswordManager;

public class DataExtractorFactoryImplTest
{
    private PasswordManager m_PasswordManager;
    private DataExtractorFactoryImpl m_Factory;

    public DataExtractorFactoryImplTest() throws NoSuchAlgorithmException
    {
        m_PasswordManager = new PasswordManager("AES/CBC/PKCS5Padding",
                new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });
        m_Factory = new DataExtractorFactoryImpl(m_PasswordManager);
    }

    @Test
    public void testNewExtractor_GivenDataSourceIsElasticsearch_NoAggs_NoAuth()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);
        dataDescription.setTimeField("time");

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("foo"));
        schedulerConfig.setTypes(Arrays.asList("bar"));
        Map<String, Object> query = new HashMap<>();
        query.put("match_all", new HashMap<String, Object>());
        schedulerConfig.setQuery(query);

        JobDetails job = new JobDetails();
        job.setDataDescription(dataDescription);
        job.setSchedulerConfig(schedulerConfig);

        DataExtractor dataExtractor = m_Factory.newExtractor(job);

        assertTrue(dataExtractor instanceof ElasticsearchDataExtractor);
        assertNull(m_Factory.createBasicAuthHeader(null, null));
        assertEquals("\"match_all\":{}", m_Factory.stringifyElasticsearchQuery(query));
        assertNull(m_Factory.stringifyElasticsearchAggregations(null, null));
        assertNull(m_Factory.stringifyElasticsearchScriptFields(null));
    }

    @Test
    public void testNewExtractor_GivenDataSourceIsElasticsearch_NoAggs_Auth()
            throws GeneralSecurityException
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);
        dataDescription.setTimeField("time");

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("foo"));
        schedulerConfig.setTypes(Arrays.asList("bar"));
        schedulerConfig.setUsername("dave");
        schedulerConfig.setPassword("my_password!");
        m_PasswordManager.secureStorage(schedulerConfig);
        Map<String, Object> query = new HashMap<>();
        query.put("match_all", new HashMap<String, Object>());
        schedulerConfig.setQuery(query);

        JobDetails job = new JobDetails();
        job.setDataDescription(dataDescription);
        job.setSchedulerConfig(schedulerConfig);

        DataExtractor dataExtractor = m_Factory.newExtractor(job);

        assertTrue(dataExtractor instanceof ElasticsearchDataExtractor);
        assertNotNull(m_Factory.createBasicAuthHeader(schedulerConfig.getUsername(), schedulerConfig.getEncryptedPassword()));
        assertEquals("\"match_all\":{}", m_Factory.stringifyElasticsearchQuery(query));
        assertNull(m_Factory.stringifyElasticsearchAggregations(null, null));
        assertNull(m_Factory.stringifyElasticsearchScriptFields(null));
    }

    @Test
    public void testNewExtractor_GivenDataSourceIsElasticsearch_Aggs()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);
        dataDescription.setTimeField("time");

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("foo"));
        schedulerConfig.setTypes(Arrays.asList("bar"));
        Map<String, Object> query = new HashMap<>();
        query.put("match_all", new HashMap<String, Object>());
        schedulerConfig.setQuery(query);

        // This block of nested maps builds the aggs structure required by Elasticsearch
        Map<String, Object> avg = new HashMap<>();
        avg.put("field", "responsetime");
        Map<String, Object> valueLevel = new HashMap<>();
        valueLevel.put("avg", avg);
        Map<String, Object> nestedAggs = new HashMap<>();
        nestedAggs.put("value_level", valueLevel);
        Map<String, Object> histogram = new LinkedHashMap<>();
        histogram.put("field", "time");
        histogram.put("interval", 3600000);
        Map<String, Object> timeLevel = new LinkedHashMap<>();
        timeLevel.put("histogram", histogram);
        timeLevel.put("aggs", nestedAggs);
        Map<String, Object> aggs = new HashMap<>();
        aggs.put("time_level", timeLevel);
        schedulerConfig.setAggs(aggs);

        JobDetails job = new JobDetails();
        job.setDataDescription(dataDescription);
        job.setSchedulerConfig(schedulerConfig);

        DataExtractor dataExtractor = m_Factory.newExtractor(job);

        assertTrue(dataExtractor instanceof ElasticsearchDataExtractor);
        assertEquals("\"match_all\":{}", m_Factory.stringifyElasticsearchQuery(query));
        assertEquals("\"aggs\":{\"time_level\":{\"histogram\":{\"field\":\"time\",\"interval\":3600000},\"aggs\":{\"value_level\":{\"avg\":{\"field\":\"responsetime\"}}}}}",
                m_Factory.stringifyElasticsearchAggregations(null, aggs));
    }

    @Test
    public void testNewExtractor_GivenDataSourceIsElasticsearch_ScriptFields()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);
        dataDescription.setTimeField("time");

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("foo"));
        schedulerConfig.setTypes(Arrays.asList("bar"));
        Map<String, Object> query = new HashMap<>();
        query.put("match_all", new HashMap<String, Object>());
        schedulerConfig.setQuery(query);

        // This block of nested maps builds the script_fields structure required by Elasticsearch
        Map<String, Object> script = new LinkedHashMap<>();
        script.put("lang", "expression");
        script.put("inline", "doc['responsetime'].value * 2");
        Map<String, Object> twiceResponseTime = new HashMap<>();
        twiceResponseTime.put("script", script);
        Map<String, Object> scriptFields = new HashMap<>();
        scriptFields.put("twiceresponsetime", twiceResponseTime);
        schedulerConfig.setScriptFields(scriptFields);

        JobDetails job = new JobDetails();
        job.setDataDescription(dataDescription);
        job.setSchedulerConfig(schedulerConfig);

        DataExtractor dataExtractor = m_Factory.newExtractor(job);

        assertTrue(dataExtractor instanceof ElasticsearchDataExtractor);
        assertEquals("\"match_all\":{}", m_Factory.stringifyElasticsearchQuery(query));
        assertEquals("\"script_fields\":{\"twiceresponsetime\":{\"script\":{\"lang\":\"expression\",\"inline\":\"doc['responsetime'].value * 2\"}}}",
                m_Factory.stringifyElasticsearchScriptFields(scriptFields));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testNewExtractor_GivenDataSourceIsFile()
    {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.FILE);

        JobDetails job = new JobDetails();
        job.setSchedulerConfig(schedulerConfig);

        m_Factory.newExtractor(job);
    }

    @Test
    public void testCreateAuthHeader_GivenNoUsername()
    {
        assertNull(m_Factory.createBasicAuthHeader(null, "my_password!"));
    }

    @Test
    public void testCreateAuthHeader_GivenNoPassword()
    {
        assertNull(m_Factory.createBasicAuthHeader("dave", null));
    }

    @Test
    public void testCreateAuthHeader_GivenUsernameAndNoPassword()
    {
        // Here "JQLd/2uaGoyOrwJW3ynShOT7e+GFsy0MAeozErZ9Wy0=" is a base 64
        // encoded encrypted version of "my_password!"
        String authHeader = m_Factory.createBasicAuthHeader("dave",
                "JQLd/2uaGoyOrwJW3ynShOT7e+GFsy0MAeozErZ9Wy0=");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
        String decoded = new String(Base64.getMimeDecoder().decode(authHeader.substring(6)), StandardCharsets.ISO_8859_1);
        assertEquals("dave:my_password!", decoded);
    }
}
