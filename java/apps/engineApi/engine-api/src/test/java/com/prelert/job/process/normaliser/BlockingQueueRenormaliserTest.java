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
package com.prelert.job.process.normaliser;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.mockito.Mockito.mock;

import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.normaliser.BlockingQueueRenormaliser;

/**
 * This class is hard to unit test because it creates
 * a normalisation process and depends on that.
 * Some refactoring is required ideally so that the
 * normaliser process isn't created inside the updateScores
 * method.
 */
public class BlockingQueueRenormaliserTest
{
    @Test
    public void testShutdown()
    {
        JobProvider jobProvider = mock(JobProvider.class);
        BlockingQueueRenormaliser normaliser = new BlockingQueueRenormaliser("foo", jobProvider);

        normaliser.shutdown(mock(Logger.class));

        assertEquals(false, normaliser.isWorkerThreadRunning());
    }


}
