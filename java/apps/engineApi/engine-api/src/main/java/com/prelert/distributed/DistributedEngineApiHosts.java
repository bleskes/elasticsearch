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
package com.prelert.distributed;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ScheduledAction;
import com.prelert.job.manager.actions.zookeeper.ZooKeeperActionGuardian;

/**
 * For distributed systems, get information about the other
 * nodes in the system and the location of running jobs
 *
 * Implements Feature to stop jetty logging an error at startup
 */
public class DistributedEngineApiHosts implements EngineApiHosts, Feature
{
    private ZooKeeperActionGuardian<Action> m_ZkActionGuard;
    private ZooKeeperActionGuardian<ScheduledAction> m_ZkSchedulerGuard;

    public DistributedEngineApiHosts(ZooKeeperActionGuardian<Action> actionGuard,
                                    ZooKeeperActionGuardian<ScheduledAction> schedulerGuard)
    {
        m_ZkActionGuard = actionGuard;
        m_ZkSchedulerGuard = schedulerGuard;
    }

    @Override
    public List<String> engineApiHosts()
    {
        return m_ZkActionGuard.engineApiHosts();
    }

    @Override
    public Map<String, String> hostByActiveJob()
    {
        return m_ZkActionGuard.hostByJob();
    }

    @Override
    public Map<String, String> hostByScheduledJob()
    {
        return m_ZkSchedulerGuard.hostByJob();
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        return false;
    }
}
