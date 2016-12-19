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
        assertEquals(false, query.isExpand());
        assertEquals(0.0, query.getAnomalyScoreFilter(), 0.0001);
        assertEquals(0.0, query.getNormalizedProbability(), 0.0001);
        assertNull(query.getStart());
        assertNull(query.getEnd());
        assertEquals("timestamp", query.getSortField());
        assertFalse(query.isSortDescending());
    }

    public void testAll() {
        BucketsQueryBuilder.BucketsQuery query = new BucketsQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .start("1000")
                .end("2000")
                .partitionValue("foo")
                .sortField("anomaly_score")
                .sortDescending(true)
                .build();

        assertEquals(20, query.getFrom());
        assertEquals(40, query.getSize());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(true, query.isExpand());
        assertEquals(50.0d, query.getAnomalyScoreFilter(), 0.00001);
        assertEquals(70.0d, query.getNormalizedProbability(), 0.00001);
        assertEquals("1000", query.getStart());
        assertEquals("2000", query.getEnd());
        assertEquals("foo", query.getPartitionValue());
        assertEquals("anomaly_score", query.getSortField());
        assertTrue(query.isSortDescending());
    }

    public void testEqualsHash() {
        BucketsQueryBuilder query = new BucketsQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .start("1000")
                .end("2000")
                .partitionValue("foo");

        BucketsQueryBuilder query2 = new BucketsQueryBuilder()
                .from(20)
                .size(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .start("1000")
                .end("2000")
                .partitionValue("foo");

        assertEquals(query.build(), query2.build());
        assertEquals(query.build().hashCode(), query2.build().hashCode());
        query2.clear();
        assertFalse(query.build().equals(query2.build()));

        query2.from(20)
        .size(40)
        .includeInterim(true)
        .expand(true)
        .anomalyScoreThreshold(50.0d)
        .normalizedProbabilityThreshold(70.0d)
        .start("1000")
        .end("2000")
        .partitionValue("foo");
        assertEquals(query.build(), query2.build());

        query2.clear();
        query2.from(20)
        .size(40)
        .includeInterim(true)
        .expand(true)
        .anomalyScoreThreshold(50.1d)
        .normalizedProbabilityThreshold(70.0d)
        .start("1000")
        .end("2000")
        .partitionValue("foo");
        assertFalse(query.build().equals(query2.build()));
    }
}