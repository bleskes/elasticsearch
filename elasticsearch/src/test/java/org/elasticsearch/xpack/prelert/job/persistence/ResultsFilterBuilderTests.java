/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

public class ResultsFilterBuilderTests extends ESTestCase {
    private static final String TIMESTAMP = "timestamp";

    public void testBuild_GivenNoFilters() {
        QueryBuilder fb = new ResultsFilterBuilder().build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    public void testBuild_GivenFilterInCtorButNoAdditionalFilters() {
        QueryBuilder originalFilter = QueryBuilders.existsQuery("someField");
        QueryBuilder fb = new ResultsFilterBuilder(originalFilter).build();

        assertEquals(originalFilter.toString(), fb.toString());
    }

    public void testBuild_GivenOnlyStartTime() {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(Bucket.TIMESTAMP.getPreferredName())
                .gte(1000);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(Bucket.TIMESTAMP.getPreferredName(), 1000, null)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_GivenOnlyEndTime() {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(TIMESTAMP)
                .lt(2000);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, null, 2000)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_GivenStartAndEndTime() {
        QueryBuilder expected = QueryBuilders
                .rangeQuery(TIMESTAMP)
                .gte(40)
                .lt(50);

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(TIMESTAMP, 40, 50)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_GivenZeroScore() {
        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", 0.0)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    public void testBuild_GivenNegativeScore() {
        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", -10.0)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    public void testBuild_GivenPositiveScore() {
        QueryBuilder expected = QueryBuilders
                .rangeQuery("someField")
                .gte(40.3);

        QueryBuilder fb = new ResultsFilterBuilder()
                .score("someField", 40.3)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_GivenInterimTrue() {
        QueryBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", true)
                .build();

        assertEquals(QueryBuilders.matchAllQuery().toString(), fb.toString());
    }

    public void testBuild_GivenInterimFalse() {
        QueryBuilder expected = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("isInterim",
                Boolean.TRUE.toString()));

        QueryBuilder fb = new ResultsFilterBuilder()
                .interim("isInterim", false)
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_TermQuery() {
        QueryBuilder expected = QueryBuilders.termQuery("fruit", "banana");

        QueryBuilder fb = new ResultsFilterBuilder()
                .term("fruit", "banana")
                .build();

        assertEquals(expected.toString(), fb.toString());
    }

    public void testBuild_GivenCombination() {
        QueryBuilder originalFilter = QueryBuilders.existsQuery("someField");
        QueryBuilder timeFilter = QueryBuilders
                .rangeQuery(Bucket.TIMESTAMP.getPreferredName())
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
                .filter(originalFilter)
                .filter(timeFilter)
                .filter(score1Filter)
                .filter(score2Filter)
                .filter(interimFilter)
                .filter(termFilter);

        QueryBuilder fb = new ResultsFilterBuilder(originalFilter)
                .timeRange(Bucket.TIMESTAMP.getPreferredName(), 1000, 2000)
                .score("score1", 50.0)
                .score("score2", 80.0)
                .interim("isInterim", false)
                .term("airline", "AAL")
                .build();

        assertEquals(expected.toString(), fb.toString());
    }
}
