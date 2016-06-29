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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.Detector;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdaterTest
{
    private static final String JOB_ID = "foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    @Mock private Auditor m_Auditor;
    private JobDetails m_Job;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);
        m_Job = new JobDetails();
        m_Job.setId(JOB_ID);
        when(m_JobManager.audit(anyString())).thenReturn(m_Auditor);
        when(m_JobManager.getJobOrThrowIfUnknown(JOB_ID)).thenReturn(m_Job);
    }

    @Test
    public void testUpdate_GivenUnknownJob() throws JobException
    {
        when(m_JobManager.getJobOrThrowIfUnknown("bar")).thenThrow(new UnknownJobException("bar"));

        m_ExpectedException.expect(UnknownJobException.class);

        new JobUpdater(m_JobManager, "bar").update("");
    }

    @Test
    public void testUpdate_GivenEmptyString() throws JobException
    {
        String update = "";

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the job update");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenNoObject() throws JobException
    {
        String update = "\"description\":\"foobar\"";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Update requires JSON that contains an object");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenInvalidKey() throws JobException
    {
        String update = "{\"dimitris\":\"foobar\"}";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid key 'dimitris'");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_UPDATE_KEY));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenValidDescriptionUpdate() throws JobException
    {
        String update = "{\"description\":\"foobar\"}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
        verify(m_JobManager).audit("foo");
        verify(m_Auditor).info("Job updated: [description]");
    }

    @Test
    public void testUpdate_GivenTwoValidUpdates() throws JobException
    {
        String update = "{\"description\":\"foobar\", \"modelDebugConfig\":{\"boundsPercentile\":33.9}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
        verify(m_JobManager).setModelDebugConfig("foo", new ModelDebugConfig(null, 33.9, null));

        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 33.9\nterms = \n";
        verify(m_JobManager).writeUpdateConfigMessage("foo", expectedConfig);
    }

    @Test
    public void testUpdate_GivenTwoUpdatesSecondBeingInvalid_ShouldApplyNone() throws JobException
    {
        String update = "{\"description\":\"foobar\", \"modelDebugConfig\":{\"boundsPercentile\":1000.0}}";

        try
        {
            new JobUpdater(m_JobManager, "foo").update(update);
            fail();
        }
        catch (JobConfigurationException e)
        {
            assertEquals("Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]", e.getMessage());
        }

        verify(m_JobManager).getJobOrThrowIfUnknown(JOB_ID);
        Mockito.verifyNoMoreInteractions(m_JobManager);
    }

    @Test
    public void testUpdate_GivenValidBackgroundPersistIntervalUpdate() throws JobException
    {
        String update = "{\"backgroundPersistInterval\": 7200}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setBackgroundPersistInterval("foo", 7200L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidCustomSettingsUpdate() throws JobException
    {
        String update = "{\"customSettings\": {\"radio\":\"head\"}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        Map<String, Object> expected = new HashMap<>();
        expected.put("radio", "head");
        verify(m_JobManager).updateCustomSettings("foo", expected);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidIgnoreDowntimeUpdate() throws JobException
    {
        String update = "{\"ignoreDowntime\": \"always\"}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).updateIgnoreDowntime("foo", IgnoreDowntime.ALWAYS);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidRenormalizationWindowDaysUpdate() throws JobException
    {
        String update = "{\"renormalizationWindowDays\": 3}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setRenormalizationWindowDays("foo", 3L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidModelSnapshotRetentionDaysUpdate() throws JobException
    {
        String update = "{\"modelSnapshotRetentionDays\": 9}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setModelSnapshotRetentionDays("foo", 9L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidResultsRetentionDaysUpdate() throws JobException
    {
        String update = "{\"resultsRetentionDays\": 3}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setResultsRetentionDays("foo", 3L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidDetectorDescriptionUpdate() throws JobException
    {
        String update = "{\"detectors\": [{\"index\":0,\"description\":\"the A train\"}]}";

        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(new Detector()));
        m_Job.setAnalysisConfig(analysisConfig);

        when(m_JobManager.updateDetectorDescription("foo", 0, "the A train")).thenReturn(true);

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).updateDetectorDescription("foo", 0, "the A train");
    }

    @Test
    public void testUpdate_GivenValidSchedulerConfigUpdate() throws JobException
    {
        when(m_JobManager.isScheduledJob("foo")).thenReturn(true);
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        m_Job.setSchedulerConfig(schedulerConfig);
        String update = "{\"schedulerConfig\": {"
                + "\"dataSource\":\"ELASTICSEARCH\","
                + "\"dataSourceCompatibility\":\"1.7.x\","
                + "\"baseUrl\":\"http://localhost:9200\","
                + "\"indexes\":[\"index1\", \"index2\"],"
                + "\"types\":[\"type1\", \"type2\"]"
                + "}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        SchedulerConfig expected = new SchedulerConfig();
        expected.setDataSource(DataSource.ELASTICSEARCH);
        expected.setDataSourceCompatibility("1.7.x");
        expected.setBaseUrl("http://localhost:9200");
        expected.setIndexes(Arrays.asList("index1", "index2"));
        expected.setTypes(Arrays.asList("type1", "type2"));
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> subQuery = new HashMap<>();
        query.put("match_all", subQuery);
        expected.setQuery(query);
        expected.setQueryDelay(60L);
        expected.setRetrieveWholeSource(false);
        expected.setScrollSize(1000);

        verify(m_JobManager).updateSchedulerConfig("foo", expected);
    }

    @Test
    public void testUpdate_GivenValidAnalysisLimitsUpdate() throws JobException
    {
        AnalysisLimits analysisLimits = new AnalysisLimits();
        analysisLimits.setModelMemoryLimit(100L);
        analysisLimits.setCategorizationExamplesLimit(4L);
        m_Job.setStatus(JobStatus.CLOSED);
        m_Job.setAnalysisLimits(analysisLimits);

        String update = "{\"analysisLimits\": {"
                + "\"modelMemoryLimit\":1000,"
                + "\"categorizationExamplesLimit\":10"
                + "}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        AnalysisLimits newLimits = new AnalysisLimits();
        newLimits.setModelMemoryLimit(1000L);
        newLimits.setCategorizationExamplesLimit(10L);
        verify(m_JobManager).setAnalysisLimits("foo", newLimits);
    }
}
