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

import org.elasticsearch.test.ESTestCase;

public class BucketsQueryBuilderTests extends ESTestCase {

    public void testDefaultBuild() throws Exception {
        BucketsQueryBuilder.BucketsQuery query = new BucketsQueryBuilder().build();

        assertEquals(0, query.getSkip());
        assertEquals(BucketsQueryBuilder.DEFAULT_TAKE_SIZE, query.getTake());
        assertEquals(false, query.isIncludeInterim());
        assertEquals(false, query.isExpand());
        assertEquals(0.0, query.getAnomalyScoreFilter(), 0.0001);
        assertEquals(0.0, query.getNormalizedProbability(), 0.0001);
        assertNull(query.getEpochStart());
        assertNull(query.getEpochEnd());
        assertEquals("timestamp", query.getSortField());
        assertFalse(query.isSortDescending());
    }

    public void testAll() {
        BucketsQueryBuilder.BucketsQuery query = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart("1000")
                .epochEnd("2000")
                .partitionValue("foo")
                .sortField("anomalyScore")
                .sortDescending(true)
                .build();

        assertEquals(20, query.getSkip());
        assertEquals(40, query.getTake());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(true, query.isExpand());
        assertEquals(50.0d, query.getAnomalyScoreFilter(), 0.00001);
        assertEquals(70.0d, query.getNormalizedProbability(), 0.00001);
        assertEquals("1000", query.getEpochStart());
        assertEquals("2000", query.getEpochEnd());
        assertEquals("foo", query.getPartitionValue());
        assertEquals("anomalyScore", query.getSortField());
        assertTrue(query.isSortDescending());
    }

    public void testEqualsHash() {
        BucketsQueryBuilder query = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart("1000")
                .epochEnd("2000")
                .partitionValue("foo");

        BucketsQueryBuilder query2 = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart("1000")
                .epochEnd("2000")
                .partitionValue("foo");

        assertEquals(query.build(), query2.build());
        assertEquals(query.build().hashCode(), query2.build().hashCode());
        query2.clear();
        assertFalse(query.build().equals(query2.build()));

        query2.skip(20)
        .take(40)
        .includeInterim(true)
        .expand(true)
        .anomalyScoreThreshold(50.0d)
        .normalizedProbabilityThreshold(70.0d)
        .epochStart("1000")
        .epochEnd("2000")
        .partitionValue("foo");
        assertEquals(query.build(), query2.build());

        query2.clear();
        query2.skip(20)
        .take(40)
        .includeInterim(true)
        .expand(true)
        .anomalyScoreThreshold(50.1d)
        .normalizedProbabilityThreshold(70.0d)
        .epochStart("1000")
        .epochEnd("2000")
        .partitionValue("foo");
        assertFalse(query.build().equals(query2.build()));
    }
}