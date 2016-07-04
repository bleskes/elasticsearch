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
 ***********************************************************/

package com.prelert.server.info;

import org.apache.commons.lang.SystemUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.utils.HostnameFinder;

/**
 * Static server resource information.
 *
 * In theory the member fields should be final but for
 * serialisation purposes they have both getters and setters.
 */
@JsonInclude(Include.NON_NULL)
public class ServerInfo
{
    private String m_OsName;
    private String m_OsVersion;
    private String m_Hostname;
    private Long m_TotalMemoryMb;
    private Long m_TotalDiskMb;
    private Long m_AvailableDiskMb;

    public ServerInfo()
    {
        m_OsName = SystemUtils.OS_NAME;
        m_OsVersion = SystemUtils.OS_VERSION;
        m_Hostname = HostnameFinder.findHostname();
    }

    public String getOsName()
    {
        return m_OsName;
    }

    public void setOsName(String osName)
    {
        m_OsName = osName;
    }

    public String getOsVersion()
    {
        return m_OsVersion;
    }

    public void setOsVersion(String osVersion)
    {
        m_OsVersion = osVersion;
    }

    public String getHostname()
    {
        return m_Hostname;
    }

    public void setHostname(String hostname)
    {
        m_Hostname = hostname;
    }

    public Long getTotalMemoryMb()
    {
        return m_TotalMemoryMb;
    }

    public void setTotalMemoryMb(Long memoryMb)
    {
        m_TotalMemoryMb = memoryMb;
    }

    public Long getTotalDiskMb()
    {
        return m_TotalDiskMb;
    }

    public void setTotalDiskMb(Long totalDiskMb)
    {
        m_TotalDiskMb = totalDiskMb;
    }

    public Long getAvailableDiskMb()
    {
        return m_AvailableDiskMb;
    }

    public void setAvailableDiskMb(Long availableDiskMb)
    {
        m_AvailableDiskMb = availableDiskMb;
    }
}
