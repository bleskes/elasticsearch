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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.prelert.job.manager.JobManager;
import com.prelert.utils.HostnameFinder;

/**
 * In non-distributed systems there is only one host - the local machine
 * and all jobs run on this host.
 *
 * Implements Feature to stop jetty logging an error at startup
 */
public class LocalEngineApiHosts implements EngineApiHosts, Feature
{
    private JobManager m_JobManager;
    private String m_Host;

    public LocalEngineApiHosts(JobManager jobManager)
    {
        m_JobManager = jobManager;
        m_Host = localHostname();
    }

    private String localHostname()
    {
        return HostnameFinder.findHostname();
    }

    @Override
    public List<String> engineApiHosts()
    {
        return Arrays.asList(m_Host);
    }

    @Override
    public Map<String, String> hostByRunningJob()
    {
        return mapToHost(m_JobManager.getRunningJobIds());
    }

    @Override
    public Map<String, String> hostByScheduledJob()
    {
        return mapToHost(m_JobManager.getStartedScheduledJobs());
    }

    private Map<String, String> mapToHost(List<String> jobs)
    {
        Map<String, String> result = new HashMap<>();
        for (String jobId : jobs)
        {
            result.put(jobId, m_Host);
        }
        return result;
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        return false;
    }
}
