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

package com.prelert.job.persistence;

import java.util.List;

/**
 * Generic wrapper class for a page of query results and the
 * total number of query hits.<br>
 * {@linkplain #hitCount()} is the total number of results
 * but that value may not be equal to the actual length of
 * the {@linkplain #queryResults()} list if skip & take or
 * some cursor was used in the database query.
 *
 * @param <T>
 */
public final class QueryPage<T>
{
    private List<T> m_QueryResults;
    private long m_HitCount;

    public QueryPage(List<T> queryResults, long hitCount)
    {
        m_QueryResults = queryResults;
        m_HitCount = hitCount;
    }

    public List<T> queryResults()
    {
        return m_QueryResults;
    }

    public long hitCount()
    {
        return m_HitCount;
    }
}
