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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
    Map<String, MemoryStats> m_ActiveJobs;
    List<String> m_StartedScheduledJobs;
    Double m_CpuLoad;
    Long m_HeapMemUsage;
    List<MemoryStats> m_MemoryStats;

    @JsonInclude(Include.NON_NULL)
    static public class MemoryStats
    {
        private Long m_ModelBytes;
        private String m_MemoryStatus;

        public MemoryStats()
        {
        }

        public MemoryStats(Long bytes, String status)
        {
            m_ModelBytes = bytes;
            m_MemoryStatus = status;
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
    }

    public EngineStatus()
    {
        m_ActiveJobs = new HashMap<>();
        m_StartedScheduledJobs = Collections.emptyList();
    }

    public Map<String, MemoryStats> getActiveJobs()
    {
        return m_ActiveJobs;
    }

    public void setActiveJobs(Map<String, MemoryStats> activeJobs)
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
}
