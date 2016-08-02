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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

public class ResultsFilterBuilderTest
{
    private static final String TIMESTAMP = "timestamp";

    @Test
    public void testBuild_GivenNoFilters()
    {
        QueryBuilder fb = new ResultsFilterBuilder().build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenFilterInCtorButNoAdditionalFilters()
    {
        QueryBuilder originalFilter = QueryBuilders.existsQuery("someField") ;
        QueryBuilder fb = new ResultsFilterBuilder(originalFilter).build();

        assertEquals(originalFilter.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenZeroStartAndEndTime()
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 0, 0)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenOnlyStartTime()
    {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(ElasticsearchMappings.ES_TIMESTAMP)
                .gte(1000);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, 1000, 0)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenOnlyEndTime()
    {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(TIMESTAMP)
                .lt(2000);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 0, 2000)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenStartAndEndTime()
    {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(TIMESTAMP)
                .gte(40)
                .lt(50);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 40, 50)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenNegativeStartAndEndTime()
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, -10, -5)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenZeroScore()
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", 0.0)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenNegativeScore()
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", -10.0)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenPositiveScore()
    {
        QueryBuilder expected = QueryBuilders
                .rangeQuery("someField")
                .gte(40.3);

        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", 40.3)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenInterimTrue()
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", true)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenInterimFalse()
    {
        QueryBuilder expected = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("isInterim",
                Boolean.TRUE.toString()));

        QueryBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", false)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_TermQuery()
    {
        QueryBuilder expected = QueryBuilders.termQuery("fruit", "banana");

        QueryBuilder fb = new ResultsFilterBuilder()
                .term("fruit", "banana")
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    @Test
    public void testBuild_GivenCombination()
    {
        QueryBuilder originalFilter = QueryBuilders.existsQuery("someField");
        QueryBuilder timeFilter = QueryBuilders
                .rangeQuery(ElasticsearchMappings.ES_TIMESTAMP)
                .gte(1000)
                .lt(2000);
        QueryBuilder score1Filter = new ResultsFilterBuilder()
                .score("score1", 50.0)
                .build();
        QueryBuilder score2Filter = new ResultsFilterBuilder()
                .score("score2", 80.0)
                .build();
        QueryBuilder interimFilter = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(
                "isInterim", Boolean.TRUE.toString()));
        QueryBuilder termFilter = QueryBuilders.termQuery("airline", "AAL");
        QueryBuilder expected = QueryBuilders.boolQuery()
                .must(originalFilter)
                .must(timeFilter)
                .must(score1Filter)
                .must(score2Filter)
                .must(interimFilter)
                .must(termFilter);

        QueryBuilder fb = new ResultsFilterBuilder(originalFilter)
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, 1000, 2000)
                .score("score1", 50.0)
                .score("score2", 80.0)
                .interim("isInterim", false)
                .term("airline", "AAL")
                .build();

        assertEquals(expected.toString(), fb.toString());
    }
}
