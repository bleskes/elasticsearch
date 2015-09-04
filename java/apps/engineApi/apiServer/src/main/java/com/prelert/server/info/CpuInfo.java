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
 ***********************************************************/

package com.prelert.server.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * System CPU information
 */
@JsonInclude(Include.NON_NULL)
public class CpuInfo
{
    private String m_Vendor;
    private String m_Model;
    private Integer m_FrequencyMHz;
    private Integer m_Cores;

    public String getVendor()
    {
        return m_Vendor;
    }

    public void setVendor(String vendor)
    {
        this.m_Vendor = vendor;
    }

    public String getModel()
    {
        return m_Model;
    }

    public void setModel(String model)
    {
        this.m_Model = model;
    }

    public Integer getCores()
    {
        return m_Cores;
    }

    public Integer getFrequencyMHz()
    {
        return m_FrequencyMHz;
    }

    public void setFrequencyMHz(Integer frequencyMHz)
    {
        this.m_FrequencyMHz = frequencyMHz;
    }

    public void setCores(Integer cores)
    {
        this.m_Cores = cores;
    }

    @Override
    public String toString()
    {
        StringBuilder format = new StringBuilder();
        if (getFrequencyMHz() != null)
        {
            format.append("%1$dMHz ");
        }
        if (getVendor() != null)
        {
            format.append("%2$s ");
        }
        if (getModel() != null)
        {
            format.append("%3$s ");
        }
        if (getCores() != null)
        {
            format.append("(%4$d cores)");
        }
        return String.format(format.toString(),
                            getFrequencyMHz(), getVendor(), getModel(), getCores());
    }

}
