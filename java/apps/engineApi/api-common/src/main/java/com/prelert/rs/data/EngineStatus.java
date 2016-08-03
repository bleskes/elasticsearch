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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Engine API status.
 * <ul>
 * <li>Average CPU load</li>
 * <li>JVM Heap Memory Usage</li>
 * </ul>
 *
 */
@JsonInclude(Include.NON_NULL)
public class EngineStatus
{
    private Double m_CpuLoad;
    private Long m_HeapMemUsage;
    private Map<String, JobStats> m_RunningJobs;
    private List<String> m_StartedScheduledJobs;
    private Map<String, String> m_DataStoreConnection;
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

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (other instanceof JobStats == false)
            {
                return false;
            }
            JobStats that = (JobStats)other;
            return Objects.equals(this.m_MemoryStatus, that.m_MemoryStatus) &&
                    Objects.equals(this.m_ModelBytes, that.m_ModelBytes) &&
                    Objects.equals(this.m_UptimeSeconds, that.m_UptimeSeconds);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(m_MemoryStatus, m_ModelBytes, m_UptimeSeconds);
        }
    }

    public EngineStatus()
    {
        m_RunningJobs = new HashMap<>();
        m_StartedScheduledJobs = Collections.emptyList();
    }

    public Map<String, JobStats> getRunningJobs()
    {
        return m_RunningJobs;
    }

    public void setRunningJobs(Map<String, JobStats> activeJobs)
    {
        m_RunningJobs = activeJobs;
    }

    public List<String> getStartedScheduledJobs()
    {
        return m_StartedScheduledJobs;
    }

    public void setStartedScheduledJobs(List<String> startedScheduledJobs)
    {
        m_StartedScheduledJobs = startedScheduledJobs;
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
    public Long getJvmHeapMemoryUsage()
    {
        return m_HeapMemUsage;
    }

    public void setJvmHeapMemoryUsage(long used)
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
     * Datastore connection parameters
     * @return
     */
    public Map<String, String> getDataStoreConnection()
    {
        return m_DataStoreConnection;
    }

    public void setDataStoreConnection(Map<String, String> params)
    {
        m_DataStoreConnection = params;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof EngineStatus == false)
        {
            return false;
        }

        EngineStatus that = (EngineStatus)other;
        return Objects.equals(this.m_CpuLoad, that.m_CpuLoad) &&
                Objects.equals(this.m_HeapMemUsage, that.m_HeapMemUsage) &&
                Objects.equals(this.m_RunningJobs, that.m_RunningJobs) &&
                Objects.equals(this.m_StartedScheduledJobs, that.m_StartedScheduledJobs) &&
                Objects.equals(this.m_DataStoreConnection, that.m_DataStoreConnection) &&
                Objects.equals(this.m_EngineHosts, that.m_EngineHosts) &&
                Objects.equals(this.m_HostByJob, that.m_HostByJob);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_CpuLoad, m_HeapMemUsage, m_RunningJobs, m_StartedScheduledJobs,
                m_DataStoreConnection, m_EngineHosts, m_HostByJob);
    }
}
