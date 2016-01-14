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

package com.prelert.job;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.prelert.job.SchedulerConfig.DataSource;


public class SchedulerConfigTest
{
    /**
     * Test parsing of the opaque {@link SchedulerConfig#m_Query()} object
     */
    @Test
    public void testAnalysisConfigRequiredFields()
            throws IOException
    {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(SchedulerConfigTest.class);

        String jobConfigStr =
            "{" +
                "\"id\":\"farequote\"," +
                "\"schedulerConfig\" : {" +
                    "\"dataSource\":\"ELASTICSEARCH\"," +
                    "\"baseUrl\":\"http://localhost:9200/\"," +
                    "\"indexes\":[\"farequote\"]," +
                    "\"types\":[\"farequote\"]," +
                    "\"query\":{\"match_all\":{} }" +
                "}," +
                "\"analysisConfig\" : {" +
                    "\"bucketSpan\":3600," +
                    "\"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]," +
                    "\"influencers\" :[\"airline\"]" +
                "}," +
                "\"dataDescription\" : {" +
                    "\"format\":\"ELASTICSEARCH\"," +
                    "\"timeField\":\"@timestamp\"," +
                    "\"timeFormat\":\"epoch_ms\"" +
                "}" +
            "}";

        ObjectReader objectReader = new ObjectMapper().readerFor(JobConfiguration.class);
        JobConfiguration jobConfig = objectReader.readValue(jobConfigStr);
        assertNotNull(jobConfig);

        SchedulerConfig schedulerConfig = jobConfig.getSchedulerConfig();
        assertNotNull(schedulerConfig);

        Map<String, Object> query = schedulerConfig.getQuery();
        assertNotNull(query);

        String queryAsJson = new ObjectMapper().writeValueAsString(query);
        logger.info("Round trip of query is: " + queryAsJson);
        assertTrue(query.containsKey("match_all"));
    }

    @Test
    public void testFillDefaults_GivenDataSourceIsFile()
    {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.FILE);

        schedulerConfig.fillDefaults();

        assertEquals(new SchedulerConfig(), schedulerConfig);
    }

    @Test
    public void testFillDefaults_GivenDataSourceIsElasticsearchAndQueryIsNotNull()
    {
        SchedulerConfig originalSchedulerConfig = new SchedulerConfig();
        originalSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        originalSchedulerConfig.setQuery(new HashMap<String, Object>());

        SchedulerConfig defaultedSchedulerConfig = new SchedulerConfig();
        defaultedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        defaultedSchedulerConfig.setQuery(new HashMap<String, Object>());

        defaultedSchedulerConfig.fillDefaults();

        assertEquals(originalSchedulerConfig, defaultedSchedulerConfig);
    }

    @Test
    public void testFillDefaults_GivenDataSourceIsElasticsearchAndQueryIsNull()
    {
        SchedulerConfig expectedSchedulerConfig = new SchedulerConfig();
        expectedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        Map<String, Object> defaultQuery = new HashMap<>();
        defaultQuery.put("match_all", new HashMap<String, Object>());
        expectedSchedulerConfig.setQuery(defaultQuery);

        SchedulerConfig defaultedSchedulerConfig = new SchedulerConfig();
        defaultedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        defaultedSchedulerConfig.setQuery(null);

        defaultedSchedulerConfig.fillDefaults();

        assertEquals(expectedSchedulerConfig, defaultedSchedulerConfig);
        assertTrue(defaultedSchedulerConfig.getQuery().containsKey("match_all"));
    }
}
