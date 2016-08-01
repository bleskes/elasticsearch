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
 * are owned by Prelert Ltd. No part of this source code    *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.job.update;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;

public class CategorizationFiltersUpdaterTest
{
    private static final String CATEGORIZATION_FIELD = "myCategory";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    private JobDetails m_Job;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_Job = new JobDetails();
        Detector detector = new Detector();
        detector.setFunction("count");
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(detector));
        m_Job.setAnalysisConfig(analysisConfig);
    }

    @Test
    public void testPrepareUpdate_GivenTextNode() throws Exception
    {
        JsonNode node = TextNode.valueOf("5");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for categorizationFilters: value must be an array of strings; actual was: \"5\"");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("myJob").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenArrayWithNonTextualElements() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"a\", 3]");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for categorizationFilters: "
                + "value must be an array of strings; actual was: [\"a\",3]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("myJob").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenJobHasNoCategorizationFieldName() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"a\"]");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "categorizationFilters require setting categorizationFieldName");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME));

        createUpdater("myJob").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenInvalidRegex() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"[\"]");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "categorizationFilters contains invalid regular expression '['");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        givenCategorizationFieldName();

        createUpdater("myJob").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenValid() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"a\", \"b\"]");

        givenCategorizationFieldName();
        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.prepareUpdate(node);

        verify(m_JobManager, never()).updateCategorizationFilters("myJob", Arrays.asList("a", "b"));
    }

    @Test
    public void testCommit_GivenValid() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"a\", \"b\"]");

        givenCategorizationFieldName();
        when(m_JobManager.updateCategorizationFilters("myJob", Arrays.asList("a", "b"))).thenReturn(true);
        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateCategorizationFilters("myJob", Arrays.asList("a", "b"));
    }

    @Test
    public void testCommit_GivenNull() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("null");

        givenCategorizationFieldName();
        when(m_JobManager.updateCategorizationFilters("myJob", null)).thenReturn(true);
        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateCategorizationFilters("myJob", null);
    }

    @Test
    public void testCommit_GivenEmptyArray() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[]");

        givenCategorizationFieldName();
        when(m_JobManager.updateCategorizationFilters("myJob", null)).thenReturn(true);
        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateCategorizationFilters("myJob", null);
    }

    @Test
    public void testCommit_GivenJobManagerFailedToUpdate() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("[\"foo\",\"bar\"]");

        givenCategorizationFieldName();
        when(m_JobManager.updateCategorizationFilters("myJob", Arrays.asList("foo", "bar"))).thenReturn(false);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Update failed. Please see the logs to trace the cause of the failure");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNKNOWN_ERROR));

        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateCategorizationFilters("myJob", null);
    }

    private CategorizationFiltersUpdater createUpdater(String jobId)
    {
        m_Job.setId(jobId);
        return new CategorizationFiltersUpdater(m_JobManager, m_Job, "categorizationFilters");
    }

    private void givenCategorizationFieldName()
    {
        m_Job.getAnalysisConfig().setCategorizationFieldName(CATEGORIZATION_FIELD);
    }
}
