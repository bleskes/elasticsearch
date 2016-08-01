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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class IgnoreDowntimeUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareUpdate_GivenInvalidIntegerValue() throws JobConfigurationException
    {
        IntNode node = IntNode.valueOf(42);

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for ignoreDowntime: expected one of [NEVER, ONCE, ALWAYS]; actual was: 42");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenInvalidTextValue() throws JobConfigurationException
    {
        TextNode node = TextNode.valueOf("invalid");

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for ignoreDowntime: expected one of [NEVER, ONCE, ALWAYS]; actual was: \"invalid\"");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testCommit_GivenValidValue() throws JobException
    {
        TextNode node = TextNode.valueOf("once");

        IgnoreDowntimeUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateIgnoreDowntime("foo", IgnoreDowntime.ONCE);
    }

    private IgnoreDowntimeUpdater createUpdater(String jobId)
    {
        JobDetails job = new JobDetails();
        job.setId(jobId);
        return new IgnoreDowntimeUpdater(m_JobManager, job, "ignoreDowntime");
    }
}
