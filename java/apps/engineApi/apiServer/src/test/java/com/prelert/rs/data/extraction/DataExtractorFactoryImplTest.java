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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.prelert.data.extractor.elasticsearch.ElasticsearchDataExtractor;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.data.extraction.DataExtractor;

public class DataExtractorFactoryImplTest
{
    private DataExtractorFactoryImpl m_Factory = new DataExtractorFactoryImpl();

    @Test
    public void testNewExtractor_GivenDataSourceIsElasticsearch_NoAggs()
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
        assertEquals("\"match_all\":{}", m_Factory.stringifyElasticsearchQuery(query));
        assertNull(m_Factory.stringifyElasticsearchAggregations(null, null));
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
        Map<String, Object> histogram = new HashMap<>();
        histogram.put("field", "time");
        histogram.put("interval", 3600000);
        Map<String, Object> timeLevel = new HashMap<>();
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

    @Test (expected = IllegalArgumentException.class)
    public void testNewExtractor_GivenDataSourceIsFile()
    {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.FILE);

        JobDetails job = new JobDetails();
        job.setSchedulerConfig(schedulerConfig);

        m_Factory.newExtractor(job);
    }
}
