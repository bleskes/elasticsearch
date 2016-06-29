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

package com.prelert.rs.job.update;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class SchedulerConfigUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    private JobDetails m_Job;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareUpdate_GivenJobIsNotScheduled() throws JobException, IOException
    {
        givenJob("foo");
        when(m_JobManager.isScheduledJob("foo")).thenReturn(false);
        String update = "{}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("There is no job 'foo' with a scheduler configured");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_SCHEDULED_JOB));

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenNull() throws JobException, IOException
    {
        givenJob("foo");
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        String update = "null";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for schedulerConfig: null");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenInvalidJson() throws JobException, IOException
    {
        givenJob("foo");
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        String update = "{\"dataSour!!ce\":\"whatever\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the update value for schedulerConfig");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenDifferentDataSource() throws JobException, IOException
    {
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        SchedulerConfig existingSchedulerConfig = new SchedulerConfig();
        existingSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        givenJobWithSchedulerConfig("foo", existingSchedulerConfig);

        String update = "{\"dataSource\":\"FILE\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for schedulerConfig: dataSource cannot be changed; existing is ELASTICSEARCH, update had FILE");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenValidationError() throws JobException, IOException
    {
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        SchedulerConfig existingSchedulerConfig = new SchedulerConfig();
        existingSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        givenJobWithSchedulerConfig("foo", existingSchedulerConfig);
        String update = "{\"dataSource\":\"ELASTICSEARCH\", \"dataSourceCompatibility\":\"invalid\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid dataSourceCompatibility value 'invalid' in scheduler configuration");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdateAndCommit_GivenValid() throws JobException, IOException
    {
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        SchedulerConfig existingSchedulerConfig = new SchedulerConfig();
        existingSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        givenJobWithSchedulerConfig("foo", existingSchedulerConfig);
        String update = "{"
                + "\"dataSource\":\"ELASTICSEARCH\","
                + "\"dataSourceCompatibility\":\"2.x.x\","
                + "\"baseUrl\":\"http://localhost:9200\","
                + "\"indexes\":[\"index1\", \"index2\"],"
                + "\"types\":[\"type1\", \"type2\"],"
                + "\"query\":{\"term\":{\"airline\":\"AAL\"}},"
                + "\"scrollSize\": 10000"
                + "}";
        JsonNode node = new ObjectMapper().readTree(update);

        SchedulerConfigUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        SchedulerConfig expected = new SchedulerConfig();
        expected.setDataSource(DataSource.ELASTICSEARCH);
        expected.setDataSourceCompatibility("2.x.x");
        expected.setBaseUrl("http://localhost:9200");
        expected.setIndexes(Arrays.asList("index1", "index2"));
        expected.setTypes(Arrays.asList("type1", "type2"));
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> termQuery = new HashMap<>();
        termQuery.put("airline", "AAL");
        query.put("term", termQuery);
        expected.setQuery(query);
        expected.setQueryDelay(60L);
        expected.setRetrieveWholeSource(false);
        expected.setScrollSize(10000);

        verify(m_JobManager).updateSchedulerConfig("foo", expected);
    }

    private SchedulerConfigUpdater createUpdater()
    {
        return new SchedulerConfigUpdater(m_JobManager, m_Job, "schedulerConfig");
    }

    private void givenJob(String jobId)
    {
        givenJobWithSchedulerConfig(jobId, null);
    }

    private void givenJobWithSchedulerConfig(String jobId, SchedulerConfig schedulerConfig)
    {
        m_Job = new JobDetails();
        m_Job.setId(jobId);
        m_Job.setSchedulerConfig(schedulerConfig);
    }
}
