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

import java.io.IOException;
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
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.prelert.job.JobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;

public class CustomSettingsUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareUpdate_GivenValueIsNotAnObject() throws JobException
    {
        JsonNode node = DoubleNode.valueOf(42.0);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid update value for customSettings: value must be an object");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        new CustomSettingsUpdater(m_JobManager, "foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenObject() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("{\"a\":1}");

        new CustomSettingsUpdater(m_JobManager, "foo").prepareUpdate(node);

        Map<String, Object> expected = new HashMap<>();
        expected.put("a", 1);
        verify(m_JobManager, never()).updateCustomSettings("foo", expected);
    }

    @Test
    public void testPrepareUpdateAndCommit_GivenObject() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("{\"a\":1}");

        CustomSettingsUpdater updater = new CustomSettingsUpdater(m_JobManager, "foo");
        updater.prepareUpdate(node);
        updater.commit();

        Map<String, Object> expected = new HashMap<>();
        expected.put("a", 1);
        verify(m_JobManager).updateCustomSettings("foo", expected);
    }
}
