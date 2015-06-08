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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * List of {@linkplain DataPostResult}
 */
public class MultiDataPostResult
{
    private List<DataPostResult> m_Results;

    public MultiDataPostResult()
    {
        m_Results = new ArrayList<DataPostResult>();
    }

    public List<DataPostResult> getResults()
    {
        return m_Results;
    }

    public void setResults(List<DataPostResult> results)
    {
        this.m_Results = results;
    }

    public void addResult(DataPostResult result)
    {
        this.m_Results.add(result);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((m_Results == null) ? 0 : m_Results.hashCode());
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

        MultiDataPostResult other = (MultiDataPostResult) obj;

        return Objects.equals(this.m_Results, other.m_Results);
    }
}
