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

package com.prelert.job.persistence.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import com.google.common.base.Strings;


/**
 * This builder facilitates the creation of a {@link QueryBuilder} with common
 * characteristics to both buckets and records.
 */
class ResultsFilterBuilder
{
    private final List<QueryBuilder> m_Filters;

    public ResultsFilterBuilder()
    {
        m_Filters = new ArrayList<>();
    }

    public ResultsFilterBuilder(QueryBuilder filterBuilder)
    {
        this();
        m_Filters.add(filterBuilder);
    }

    public ResultsFilterBuilder timeRange(String field, long startEpochMs, long endEpochMs)
    {
        if (startEpochMs > 0 || endEpochMs > 0)
        {
            RangeQueryBuilder timeRange = QueryBuilders.rangeQuery(field);

            if (startEpochMs > 0)
            {
                timeRange.gte(startEpochMs);
            }
            if (endEpochMs > 0)
            {
                timeRange.lt(endEpochMs);
            }
            addFilter(timeRange);
        }
        return this;
    }

    public ResultsFilterBuilder score(String fieldName, double threshold)
    {
        if (threshold > 0.0)
        {
            RangeQueryBuilder scoreFilter = QueryBuilders.rangeQuery(fieldName);
            scoreFilter.gte(threshold);
            addFilter(scoreFilter);
        }
        return this;
    }

    public ResultsFilterBuilder interim(String fieldName, boolean includeInterim)
    {
        if (includeInterim)
        {
            // Including interim results does not stop final results being
            // shown, so including interim results means no filtering on the
            // isInterim field
            return this;
        }

        // Implemented as "NOT isInterim == true" so that not present and null
        // are equivalent to false.  This improves backwards compatibility.
        // Also, note how for a boolean field, unlike numeric term filters, the
        // term value is supplied as a string.
        TermQueryBuilder interimFilter = QueryBuilders.termQuery(fieldName,
                Boolean.TRUE.toString());
        QueryBuilder notInterimFilter = QueryBuilders.boolQuery().mustNot(interimFilter);
        addFilter(notInterimFilter);
        return this;
    }

    public ResultsFilterBuilder term(String fieldName, String fieldValue)
    {
        if (Strings.isNullOrEmpty(fieldName) || Strings.isNullOrEmpty(fieldValue))
        {
            return this;
        }

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(fieldName, fieldValue);
        addFilter(termQueryBuilder);
        return this;
    }

    private void addFilter(QueryBuilder fb)
    {
        m_Filters.add(fb);
    }

    public QueryBuilder build()
    {
        if (m_Filters.isEmpty())
        {
            return QueryBuilders.matchAllQuery();
        }
        if (m_Filters.size() == 1)
        {
            return m_Filters.get(0);
        }
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (QueryBuilder query : m_Filters)
        {
            boolQueryBuilder.must(query);
        }
        return boolQueryBuilder;
    }
}
