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

package com.prelert.rs.data;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.job.DataCounts;

/**
 * The result of a data upload is either an error
 * or an {@linkplain DataCounts} object. This class
 * encapsulates this with the Job Id.
 */
@JsonInclude(Include.NON_NULL)
public class DataPostResult
{
    private String m_JobId;
    private DataCounts m_DataCounts;
    private ApiError m_Error;

    /**
     * For serialisation
     */
    public DataPostResult()
    {
    }

    public DataPostResult(String jobId, DataCounts counts)
    {
        m_JobId = jobId;
        m_DataCounts = counts;
    }

    public DataPostResult(String jobId, ApiError error)
    {
        m_JobId = jobId;
        m_Error = error;
    }

    public String getJobId()
    {
        return m_JobId;
    }

    public void setJobId(String jobId)
    {
        this.m_JobId = jobId;
    }

    public DataCounts getDataCounts()
    {
        return m_DataCounts;
    }

    public void setDataCounts(DataCounts dataCounts)
    {
        this.m_DataCounts = dataCounts;
    }

    public ApiError getError()
    {
        return m_Error;
    }

    public void setError(ApiError error)
    {
        this.m_Error = error;
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((m_DataCounts == null) ? 0 : m_DataCounts.hashCode());
        result = prime * result + ((m_Error == null) ? 0 : m_Error.hashCode());
        result = prime * result + ((m_JobId == null) ? 0 : m_JobId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        DataPostResult other = (DataPostResult) obj;

        return Objects.equals(this.m_DataCounts, other.m_DataCounts) &&
                   Objects.equals(this.m_JobId, other.m_JobId) &&
                   Objects.equals(this.m_Error, other.m_Error);
    }

}
