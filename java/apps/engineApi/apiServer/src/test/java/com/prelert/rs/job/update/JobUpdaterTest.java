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
import com.prelert.job.Detector;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    @Mock private Auditor m_Auditor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_JobManager.audit(anyString())).thenReturn(m_Auditor);
    }

    @Test
    public void testUpdate_GivenEmptyString() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "";

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the job update");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenNoObject() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "\"description\":\"foobar\"";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Update requires JSON that contains an object");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenInvalidKey() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"dimitris\":\"foobar\"}";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid key 'dimitris'");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_UPDATE_KEY));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenValidDescriptionUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"description\":\"foobar\"}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
        verify(m_JobManager).audit("foo");
        verify(m_Auditor).info("Job updated: [description]");
    }

    @Test
    public void testUpdate_GivenTwoValidUpdates() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"description\":\"foobar\", \"modelDebugConfig\":{\"boundsPercentile\":33.9}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
        verify(m_JobManager).setModelDebugConfig("foo", new ModelDebugConfig(null, 33.9, null));

        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 33.9\nterms = \n";
        verify(m_JobManager).writeUpdateConfigMessage("foo", expectedConfig);
    }

    @Test
    public void testUpdate_GivenTwoUpdatesSecondBeingInvalid_ShouldApplyNone()
            throws UnknownJobException, JobConfigurationException, JobInUseException,
            NativeProcessRunException
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

        Mockito.verifyNoMoreInteractions(m_JobManager);
    }

    @Test
    public void testUpdate_GivenValidBackgroundPersistIntervalUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"backgroundPersistInterval\": 7200}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setBackgroundPersistInterval("foo", 7200L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidCustomSettingsUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"customSettings\": {\"radio\":\"head\"}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        Map<String, Object> expected = new HashMap<>();
        expected.put("radio", "head");
        verify(m_JobManager).updateCustomSettings("foo", expected);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidIgnoreDowntimeUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"ignoreDowntime\": \"always\"}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).updateIgnoreDowntime("foo", IgnoreDowntime.ALWAYS);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidRenormalizationWindowDaysUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"renormalizationWindowDays\": 3}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setRenormalizationWindowDays("foo", 3L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidModelSnapshotRetentionDaysUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"modelSnapshotRetentionDays\": 9}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setModelSnapshotRetentionDays("foo", 9L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidResultsRetentionDaysUpdate() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"resultsRetentionDays\": 3}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setResultsRetentionDays("foo", 3L);
        verify(m_JobManager, never()).writeUpdateConfigMessage(anyString(), anyString());
    }

    @Test
    public void testUpdate_GivenValidDetectorDescriptionUpdate() throws JobConfigurationException,
            UnknownJobException, JobInUseException, NativeProcessRunException
    {
        String update = "{\"detectors\": [{\"index\":0,\"description\":\"the A train\"}]}";

        JobDetails job = new JobDetails();
        job.setId("foo");
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(new Detector()));
        job.setAnalysisConfig(analysisConfig);

        when(m_JobManager.updateDetectorDescription("foo", 0, "the A train")).thenReturn(true);

        when(m_JobManager.getJobOrThrowIfUnknown("foo")).thenReturn(job);

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).updateDetectorDescription("foo", 0, "the A train");
    }
}
