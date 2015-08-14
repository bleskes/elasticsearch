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

package com.prelert.job.quantiles;

import java.util.Date;
import java.util.Objects;

import org.elasticsearch.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Quantiles Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class Quantiles
{
    public static final String QUANTILES_ID = "hierarchical";
    public static final String CURRENT_VERSION = "1";

    /**
     * Field Names
     */
    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String TIMESTAMP = "timestamp";
    public static final String QUANTILE_STATE = "quantileState";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "quantiles";

    private Date m_Timestamp;
    private String m_State;

    public String getId()
    {
        return QUANTILES_ID;
    }

    public String getVersion()
    {
        return CURRENT_VERSION;
    }

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }

    public String getState()
    {
        return Strings.nullToEmpty(m_State);
    }

    public void setState(String state)
    {
        m_State = state;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(m_State);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Quantiles == false)
        {
            return false;
        }

        Quantiles that = (Quantiles) other;

        return Objects.equals(this.m_State, that.m_State);
    }
}

