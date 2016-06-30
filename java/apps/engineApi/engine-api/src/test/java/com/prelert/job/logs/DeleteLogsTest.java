/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
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

package com.prelert.job.logs;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.prelert.job.JobException;

public class DeleteLogsTest
{
    /**
     * Tests that the if the don't delete log files system property is
     * set then the logs aren't deleted.
     */
    @Test
    public void dontDeleteTest() throws JobException
    {
        System.setProperty(JobLogs.DONT_DELETE_LOGS_PROP, "true");
        JobLogs jobLogs = new JobLogs();
        assertTrue(jobLogs.deleteLogs("somedir", "somejob"));

        System.setProperty(JobLogs.DONT_DELETE_LOGS_PROP, "1");
        jobLogs = new JobLogs();
        assertTrue(jobLogs.deleteLogs("somedir", "somejob"));

        System.clearProperty(JobLogs.DONT_DELETE_LOGS_PROP);
        jobLogs = new JobLogs();
        assertFalse(jobLogs.deleteLogs("somedir", "somejob"));
    }
}
