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

package com.prelert.rs.resources;

import java.util.ArrayList;
import java.util.List;

import com.prelert.app.Shutdownable;

public class ShutdownThreadBuilder
{
    private final List<Shutdownable> m_Tasks;

    public ShutdownThreadBuilder()
    {
        m_Tasks = new ArrayList<>();
    }

    public ShutdownThreadBuilder addTask(Shutdownable task)
    {
        m_Tasks.add(task);
        return this;
    }

    public Thread build()
    {
        return new ShutdownThread(m_Tasks);
    }

    private static class ShutdownThread extends Thread
    {
        private final List<Shutdownable> m_ShutdownTasks;

        private ShutdownThread(List<Shutdownable> shutdownTasks)
        {
            m_ShutdownTasks = shutdownTasks;
        }

        @Override
        public void run()
        {
            for (Shutdownable task : m_ShutdownTasks)
            {
                synchronized (task)
                {
                    task.shutdown();
                }
            }
        }
    }
}
