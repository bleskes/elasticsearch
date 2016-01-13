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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


public class SchedulerConfigTest
{
    /**
     * Test parsing of the opaque {@link SchedulerConfig#m_Search()} object
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
                    "\"search\":{\"query\":{\"match_all\":{} } }" +
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

        Map<String, Object> search = schedulerConfig.getSearch();
        assertNotNull(search);

        String searchAsJson = new ObjectMapper().writeValueAsString(search);
        logger.info("Round trip of search is: " + searchAsJson);

        assertTrue(search.containsKey("query"));
        Map<String, Object> query = (Map<String, Object>)search.get("query");
        assertTrue(query.containsKey("match_all"));
    }
}
