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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class ModelDebugConfigUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    private StringWriter m_ConfigWriter;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ConfigWriter = new StringWriter();
    }

    @Test
    public void testPrepareUpdate_GivenInvalidJson() throws JobException, IOException
    {
        String update = "{\"invalidKey\":3.0}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the update value for ModelDebugConfig");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenBoundsPercentileIsOutOfBounds() throws JobException, IOException
    {
        String update = "{\"boundsPercentile\":300.0}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenValid() throws JobException, IOException
    {
        String update = "{\"boundsPercentile\":67.3, \"terms\":\"a,b\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        createUpdater("foo").prepareUpdate(node);

        verify(m_JobManager, never()).setModelDebugConfig("foo", new ModelDebugConfig(null, 67.3, "a,b"));
    }

    @Test
    public void testCommit_GivenValid() throws JobException, IOException
    {
        String update = "{\"boundsPercentile\":67.3, \"terms\":\"a,b\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        ModelDebugConfigUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).setModelDebugConfig("foo", new ModelDebugConfig(null, 67.3, "a,b"));
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 67.3\nterms = a,b\n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    @Test
    public void testCommit_GivenNull() throws JobException, IOException
    {
        JsonNode node = NullNode.getInstance();

        ModelDebugConfigUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).setModelDebugConfig("foo", null);
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = -1.0\nterms = \n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    private ModelDebugConfigUpdater createUpdater(String jobId)
    {
        JobDetails job = new JobDetails();
        job.setId(jobId);
        return new ModelDebugConfigUpdater(m_JobManager, job, "modelDebugConfig", m_ConfigWriter);
    }
}
