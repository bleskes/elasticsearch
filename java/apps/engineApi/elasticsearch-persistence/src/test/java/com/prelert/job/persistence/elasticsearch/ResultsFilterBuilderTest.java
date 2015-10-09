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

import static org.junit.Assert.assertEquals;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.junit.Test;

public class ResultsFilterBuilderTest
{
    private static final String TIMESTAMP = "timestamp";

    @Test
    public void testBuild_GivenNoFilters()
    {
        FilterBuilder fb = new ResultsFilterBuilder().build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenFilterInCtorButNoAdditionalFilters()
    {
        FilterBuilder originalFilter = FilterBuilders.existsFilter("someField") ;
        FilterBuilder fb = new ResultsFilterBuilder(originalFilter).build();

        assertEquals(originalFilter.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenZeroStartAndEndTime()
    {
        FilterBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 0, 0)
                .build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenOnlyStartTime()
    {
        FilterBuilder expected = FilterBuilders
                .rangeFilter(ElasticsearchMappings.ES_TIMESTAMP)
                .gte(1000);

        FilterBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, 1000, 0)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenOnlyEndTime()
    {
        FilterBuilder expected = FilterBuilders
                .rangeFilter(TIMESTAMP)
                .lt(2000);

        FilterBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 0, 2000)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenStartAndEndTime()
    {
        FilterBuilder expected = FilterBuilders
                .rangeFilter(TIMESTAMP)
                .gte(40)
                .lt(50);

        FilterBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 40, 50)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenNegativeStartAndEndTime()
    {
        FilterBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, -10, -5)
                .build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenZeroScore()
    {
        FilterBuilder fb = new ResultsFilterBuilder()
                .score("someField", 0.0)
                .build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenNegativeScore()
    {
        FilterBuilder fb = new ResultsFilterBuilder()
                .score("someField", -10.0)
                .build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenPositiveScore()
    {
        FilterBuilder expected = FilterBuilders
                .rangeFilter("someField")
                .gte(40.3);

        FilterBuilder fb = new ResultsFilterBuilder()
                .score("someField", 40.3)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenInterimTrue()
    {
        FilterBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", true)
                .build();

        assertEquals(FilterBuilders.matchAllFilter().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenInterimFalse()
    {
        FilterBuilder expected = FilterBuilders.notFilter(FilterBuilders.termFilter("isInterim",
                Boolean.TRUE.toString()));

        FilterBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", false)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenCombination()
    {
        FilterBuilder originalFilter = FilterBuilders.existsFilter("someField");
        FilterBuilder timeFilter = FilterBuilders
                .rangeFilter(ElasticsearchMappings.ES_TIMESTAMP)
                .gte(1000)
                .lt(2000);
        FilterBuilder score1Filter = new ResultsFilterBuilder()
                .score("score1", 50.0)
                .build();
        FilterBuilder score2Filter = new ResultsFilterBuilder()
                .score("score2", 80.0)
                .build();
        FilterBuilder interimFilter = FilterBuilders.notFilter(FilterBuilders.termFilter(
                "isInterim", Boolean.TRUE.toString()));
        FilterBuilder expected = FilterBuilders.andFilter(originalFilter, timeFilter, score1Filter,
                score2Filter, interimFilter);

        FilterBuilder fb = new ResultsFilterBuilder(originalFilter)
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, 1000, 2000)
                .score("score1", 50.0)
                .score("score2", 80.0)
                .interim("isInterim", false)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }
}
