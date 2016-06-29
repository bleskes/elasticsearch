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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;

public class ResultsRetentionDaysUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareUpdate_GivenText() throws JobException, IOException
    {
        JsonNode node = TextNode.valueOf("5");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for resultsRetentionDays: value must be an exact number of days");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenNegativeInteger() throws JobException, IOException
    {
        JsonNode node = LongNode.valueOf(-3);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for resultsRetentionDays: value must be an exact number of days");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenInteger() throws JobException, IOException
    {
        JsonNode node = LongNode.valueOf(5);

        createUpdater("foo").prepareUpdate(node);

        verify(m_JobManager, never()).setResultsRetentionDays("foo", 5L);
    }

    @Test
    public void testCommit_GivenInteger() throws JobException, IOException
    {
        JsonNode node = LongNode.valueOf(5);

        ResultsRetentionDaysUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).setResultsRetentionDays("foo", 5L);
    }

    @Test
    public void testCommit_GivenNull() throws JobException, IOException
    {
        JsonNode node = NullNode.getInstance();

        ResultsRetentionDaysUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).setResultsRetentionDays("foo", null);
    }

    private ResultsRetentionDaysUpdater createUpdater(String jobId)
    {
        JobDetails job = new JobDetails();
        job.setId(jobId);
        return new ResultsRetentionDaysUpdater(m_JobManager, job, "resultsRetentionDays");
    }
}
