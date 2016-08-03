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
package com.prelert.job;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JobStatusTest
{
    @Test
    public void testIsAnyOf()
    {
        assertFalse(JobStatus.RUNNING.isAnyOf());
        assertFalse(JobStatus.RUNNING.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));
        assertFalse(JobStatus.CLOSED.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));

        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING));
        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSED));
        assertTrue(JobStatus.PAUSED.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
        assertTrue(JobStatus.PAUSING.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
    }
}
