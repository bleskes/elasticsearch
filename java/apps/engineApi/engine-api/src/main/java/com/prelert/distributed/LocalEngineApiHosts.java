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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;

import com.prelert.job.manager.JobManager;

/**
 * In non-distributed systems there is only one host - the local machine
 * and all jobs run on this host.
 *
 * Implements Feature to stop jetty logging an error at startup
 */
public class LocalEngineApiHosts implements EngineApiHosts, Feature
{
    private static final Logger LOGGER = Logger.getLogger(LocalEngineApiHosts.class);

    private JobManager m_JobManager;
    private String m_Host;

    public LocalEngineApiHosts(JobManager jobManager)
    {
        m_JobManager = jobManager;
        m_Host = localHostname();
    }

    private String localHostname()
    {
        try
        {
            return Inet4Address.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            LOGGER.error("Cannot determine local hostname", e);
            return "localhost";
        }
    }

    @Override
    public List<String> engineApiHosts()
    {
        return Arrays.asList(m_Host);
    }

    @Override
    public Map<String, String> hostByActiveJob()
    {
        Map<String, String> result = new HashMap<>();

        List<String> activeJobs = m_JobManager.getActiveJobIds();
        for (String jobId : activeJobs)
        {
            result.put(jobId, m_Host);
        }

        return result;
    }

    @Override
    public Map<String, String> hostByScheduledJob()
    {
        Map<String, String> result = new HashMap<>();

        List<String> scheduledJobs = m_JobManager.getStartedScheduledJobs();
        for (String jobId : scheduledJobs)
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
