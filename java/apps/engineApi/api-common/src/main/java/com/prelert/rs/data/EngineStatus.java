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

package com.prelert.rs.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Engine API status.
 * <ul>
 * <li>Number of Active jobs (scheduled and running)</li>
 * <li>List of Active job IDs (scheduled and running)</li>
 * <li>Average CPU load</li>
 * </ul>
 *
 */
@JsonInclude(Include.NON_NULL)
public class EngineStatus
{
    private Map<String, JobStats> m_ActiveJobs;
    private List<String> m_StartedScheduledJobs;
    private Double m_CpuLoad;
    private Long m_HeapMemUsage;
    private Map<String, String> m_DbConnection;
    private List<String> m_EngineHosts;
    private Map<String, String> m_HostByJob;

    @JsonInclude(Include.NON_NULL)
    public static class JobStats
    {
        private Long m_ModelBytes;
        private String m_MemoryStatus;
        private Long m_UptimeSeconds;

        public JobStats()
        {
            // Default constructor
        }

        public JobStats(Long bytes, String status, Long uptime)
        {
            m_ModelBytes = bytes;
            m_MemoryStatus = status;
            m_UptimeSeconds = uptime;
        }

        public Long getModelBytes()
        {
            return m_ModelBytes;
        }

        public void setModelBytes(Long bytes)
        {
            m_ModelBytes = bytes;
        }

        public String getMemoryStatus()
        {
            return m_MemoryStatus;
        }

        public void setMemoryStatus(String status)
        {
            m_MemoryStatus = status;
        }

        public Long getUptimeSeconds()
        {
            return m_UptimeSeconds;
        }

        public void setUptimeSeconds(long uptime)
        {
            m_UptimeSeconds = uptime;
        }
    }

    public EngineStatus()
    {
        m_ActiveJobs = new HashMap<>();
        m_StartedScheduledJobs = Collections.emptyList();
    }

    public Map<String, JobStats> getActiveJobs()
    {
        return m_ActiveJobs;
    }

    public void setActiveJobs(Map<String, JobStats> activeJobs)
    {
        m_ActiveJobs = activeJobs;
    }

    public List<String> getStartedScheduledJobs()
    {
        return m_StartedScheduledJobs;
    }

    public void setStartedScheduledJobs(List<String> startedScheduledJobs)
    {
        m_StartedScheduledJobs = startedScheduledJobs;
    }

    @JsonProperty
    public int getActiveJobCount()
    {
        return m_ActiveJobs.size();
    }

    /**
     * Calculated field only present to keep jackson serialisation happy.
     * This property should not be deserialised
     * @param count
     */
    @JsonIgnore
    private void setActiveJobCount(int count)
    {
        throw new IllegalStateException();
    }

    /**
     * Average CPU load for the last minute
     * @return The load average or -1 if not available
     */
    public Double getAverageCpuLoad()
    {
        return m_CpuLoad;
    }

    public void setAverageCpuLoad(double load)
    {
        m_CpuLoad = load;
    }

    /**
     * Heap memory in use (bytes)
     * @return
     */
    public Long getHeapMemoryUsage()
    {
        return m_HeapMemUsage;
    }

    public void setHeapMemoryUsage(long used)
    {
        m_HeapMemUsage = used;
    }

    /**
     * List of participating Engine API hosts
     * @return
     */
    public List<String> getEngineHosts()
    {
        return m_EngineHosts;
    }

    public void setEngineHosts(List<String> hosts)
    {
        m_EngineHosts = hosts;
    }

    /**
     * Map of Job ID to the host it is running on
     * @return
     */
    public Map<String, String> getHostByJob()
    {
        return m_HostByJob;
    }

    public void setHostByJob(Map<String, String> hostByJob)
    {
        this.m_HostByJob = hostByJob;
    }

    /**
     * Database connection parameters
     * @return
     */
    public Map<String, String> getDbConnection()
    {
        return m_DbConnection;
    }

    public void setDbConnection(Map<String, String> params)
    {
        m_DbConnection = params;
    }
}


