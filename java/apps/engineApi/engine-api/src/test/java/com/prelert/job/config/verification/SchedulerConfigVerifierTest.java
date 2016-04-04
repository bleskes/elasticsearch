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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    public void testVerify_GivenAllDataSources_DoesNotThrowIllegalStateException() throws JobConfigurationException
    {
        for (DataSource dataSource : DataSource.values())
        {
            SchedulerConfig conf = new SchedulerConfig();
            conf.setDataSource(dataSource);

            try
            {
                SchedulerConfigVerifier.verify(conf);
            }
            catch (JobConfigurationException e)
            {
                // Expected
            }
        }
    }

    @Test
    public void testCheckValidFile_AllOk() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("myfile.csv");

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
        conf.setFilePath("");

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
        conf.setFilePath("myfile.csv");
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
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>(){}));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_WithUsernameAndPassword() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>(){}));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_WithUsernameAndEncryptedPassword() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setEncryptedPassword("already_encrypted");
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>(){}));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_WithPasswordNoUsername() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setPassword("secret");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_BothPasswordAndEncryptedPassword() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        conf.setEncryptedPassword("already_encrypted");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_NoQuery() throws JobConfigurationException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_InappropriateField() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>(){}));
        conf.setTailFile(true);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenScriptFieldsNotWholeSource() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>(){}));
        conf.setRetrieveWholeSource(false);

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_GivenScriptFieldsAndWholeSource() throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>(){}));
        conf.setRetrieveWholeSource(true);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNullIndexes() throws JobConfigurationException,
            IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(null);
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid indexes value 'null' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenEmptyIndexes() throws JobConfigurationException,
            IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Collections.emptyList());
        conf.setTypes(Arrays.asList("mytype"));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid indexes value '[]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyNulls()
            throws JobConfigurationException, IOException
    {
        List<String> indexes = new ArrayList<>();
        indexes.add(null);
        indexes.add(null);

        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid indexes value '[null, null]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyEmptyStrings()
            throws JobConfigurationException, IOException
    {
        List<String> indexes = new ArrayList<>();
        indexes.add("");
        indexes.add("");

        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid indexes value '[, ]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNegativeQueryDelay()
            throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(-10L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid queryDelay value");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNegativeFrequency()
            throws JobConfigurationException, IOException
    {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setFrequency(-600L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        m_ExpectedException.expectMessage("Invalid frequency value");

        SchedulerConfigVerifier.verify(conf);
    }
}
