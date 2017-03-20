/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.results.Influencer;

public class InfluencersQueryBuilderTests extends ESTestCase {

    public void testDefaultBuild() throws Exception {
        InfluencersQueryBuilder.InfluencersQuery query = new InfluencersQueryBuilder().build();

        assertEquals(0, query.getFrom());
        assertEquals(InfluencersQueryBuilder.DEFAULT_SIZE, query.getSize());
        assertEquals(false, query.isIncludeInterim());
        assertEquals(0.0, query.getAnomalyScoreFilter(), 0.0001);
        assertNull(query.getStart());
        assertNull(query.getEnd());
        assertEquals(Influencer.INFLUENCER_SCORE.getPreferredName(), query.getSortField());
        assertFalse(query.isSortDescending());
    }

    public void testAll() {
        InfluencersQueryBuilder.InfluencersQuery query = new InfluencersQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .anomalyScoreThreshold(50.0d)
                .start("1000")
                .end("2000")
                .sortField("anomaly_score")
                .sortDescending(true)
                .build();

        assertEquals(20, query.getFrom());
        assertEquals(40, query.getSize());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(50.0d, query.getAnomalyScoreFilter(), 0.00001);
        assertEquals("1000", query.getStart());
        assertEquals("2000", query.getEnd());
        assertEquals("anomaly_score", query.getSortField());
        assertTrue(query.isSortDescending());
    }

    public void testEqualsHash() {
        InfluencersQueryBuilder query = new InfluencersQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .anomalyScoreThreshold(50.0d)
                .start("1000")
                .end("2000");

        InfluencersQueryBuilder query2 = new InfluencersQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .anomalyScoreThreshold(50.0d)
                .start("1000")
                .end("2000");

        assertEquals(query.build(), query2.build());
        assertEquals(query.build().hashCode(), query2.build().hashCode());
        query2.clear();
        assertFalse(query.build().equals(query2.build()));

        query2.from(20)
        .size(40)
        .includeInterim(true)
        .anomalyScoreThreshold(50.0d)
        .start("1000")
        .end("2000");
        assertEquals(query.build(), query2.build());

        query2.clear();
        query2.from(20)
        .size(40)
        .includeInterim(true)
        .anomalyScoreThreshold(50.1d)
        .start("1000")
        .end("2000");
        assertFalse(query.build().equals(query2.build()));
    }
}
