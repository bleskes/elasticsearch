/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate_GivenEmptyString() throws UnknownJobException, JobConfigurationException
    {
        String update = "";

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the job update");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenNoObject() throws UnknownJobException, JobConfigurationException
    {
        String update = "\"description\":\"foobar\"";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Update requires JSON that contains an object");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_CONFIG_PARSE_ERROR));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenInvalidKey() throws UnknownJobException, JobConfigurationException
    {
        String update = "{\"dimitris\":\"foobar\"}";

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid key 'dimitris'. Valid keys for update are: description, modelDebugConfig");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_UPDATE_KEY));

        new JobUpdater(m_JobManager, "foo").update(update);
    }

    @Test
    public void testUpdate_GivenValidDescriptionUpdate() throws UnknownJobException, JobConfigurationException
    {
        String update = "{\"description\":\"foobar\"}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
    }

    @Test
    public void testUpdate_GivenTwoUpdates() throws UnknownJobException, JobConfigurationException
    {
        String update = "{\"description\":\"foobar\", \"modelDebugConfig\":{\"boundsPercentile\":33.9}}";

        new JobUpdater(m_JobManager, "foo").update(update);

        verify(m_JobManager).setDescription("foo", "foobar");
        verify(m_JobManager).setModelDebugConfig("foo", new ModelDebugConfig(33.9, null));
    }
}
