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

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;

/**
 * This builder facilitates the creation of a {@link FilterBuilder} with common
 * characteristics to both buckets and records.
 */
class InfluencersFilterBuilder
{
    private final List<FilterBuilder> m_FilterBuilders = new ArrayList<>();

    public InfluencersFilterBuilder()
    {
    }

    public InfluencersFilterBuilder(FilterBuilder filterBuilder)
    {
        m_FilterBuilders.add(filterBuilder);
    }

    public InfluencersFilterBuilder timeRange(long startEpochMs, long endEpochMs)
    {
        if (startEpochMs > 0 || endEpochMs > 0)
        {
            // HACK for the timestamps being @timestamp in the database
            RangeFilterBuilder timeRange = FilterBuilders
                    .rangeFilter(ElasticsearchMappings.ES_TIMESTAMP);

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

    public InfluencersFilterBuilder score(String fieldName, double threshold)
    {
        if (threshold > 0.0)
        {
            RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(fieldName);
            scoreFilter.gte(threshold);
            addFilter(scoreFilter);
        }
        return this;
    }

    public InfluencersFilterBuilder interim(String fieldName, boolean includeInterim)
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
        TermFilterBuilder interimFilter = FilterBuilders.termFilter(fieldName,
                Boolean.TRUE.toString());
        FilterBuilder notInterimFilter = FilterBuilders.notFilter(interimFilter);
        addFilter(notInterimFilter);
        return this;
    }

    private void addFilter(FilterBuilder fb)
    {
        m_FilterBuilders.add(fb);
    }

    public FilterBuilder build()
    {
        if (m_FilterBuilders.isEmpty())
        {
            return FilterBuilders.matchAllFilter();
        }
        return m_FilterBuilders.size() == 1 ? m_FilterBuilders.get(0) : FilterBuilders
                .andFilter(m_FilterBuilders.toArray(new FilterBuilder[m_FilterBuilders.size()]));
    }
}
