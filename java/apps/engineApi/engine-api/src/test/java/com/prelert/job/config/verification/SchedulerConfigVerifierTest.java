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
package com.prelert.job.config.verification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;


public class SchedulerConfigVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testCheckValidFile_AllOk() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setPath("myfile.csv");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_NoPath() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_EmptyPath() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setPath("");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_InappropriateField() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setPath("myfile.csv");
        conf.setBaseUrl("http://localhost:9200/");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_AllOk() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(new ArrayList<String>(Arrays.asList("myindex")));
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));
        ObjectMapper mapper = new ObjectMapper();
        conf.setSearch(mapper.readValue("{ \"query\" : { \"match_all\" : {} } }", new TypeReference<Map<String, Object>>(){}));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_NoSearch() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(new ArrayList<String>(Arrays.asList("myindex")));
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_InappropriateField() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(new ArrayList<String>(Arrays.asList("myindex")));
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));
        ObjectMapper mapper = new ObjectMapper();
        conf.setSearch(mapper.readValue("{ \"query\" : { \"match_all\" : {} } }", new TypeReference<Map<String, Object>>(){}));
        conf.setTail(true);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));
        SchedulerConfigVerifier.verify(conf);
    }
}
