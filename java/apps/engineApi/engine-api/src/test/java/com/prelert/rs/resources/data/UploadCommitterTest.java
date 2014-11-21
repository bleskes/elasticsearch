/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.rs.resources.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.prelert.job.JobInUseException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.NativeProcessRunException;

public class UploadCommitterTest
{

    @Test(expected = NullPointerException.class)
    public void testConstructor_GivenNullJobManager()
    {
        new UploadCommitter(null);
    }

    @Test
    public void testCommitUpload() throws UnknownJobException, NativeProcessRunException,
            JobInUseException
    {
        JobManager jobManager = mock(JobManager.class);
        UploadCommitter uploadCommitter = new UploadCommitter(jobManager);

        uploadCommitter.commitUpload("foo");

        verify(jobManager).finishJob("foo");
    }
}
