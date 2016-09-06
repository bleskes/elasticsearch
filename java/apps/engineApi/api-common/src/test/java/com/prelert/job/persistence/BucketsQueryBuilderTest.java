/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
 ***********************************************************/
package com.prelert.job.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class BucketsQueryBuilderTest {

    @Test
    public void testDefaultBuild() throws Exception
    {
        BucketsQueryBuilder.BucketsQuery query = new BucketsQueryBuilder().build();

        assertEquals(0, query.getSkip());
        assertEquals(BucketsQueryBuilder.DEFAULT_TAKE_SIZE, query.getTake());
        assertEquals(false, query.isIncludeInterim());
        assertEquals(false, query.isExpand());
        assertEquals(0.0, query.getAnomalyScoreFilter(), 0.0001);
        assertEquals(0.0, query.getNormalizedProbability(), 0.0001);
        assertEquals(-1l, query.getEpochStart());
        assertEquals(-1l, query.getEpochEnd());
    }

    @Test
    public void testAll()
    {
        BucketsQueryBuilder.BucketsQuery query = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart(1000l)
                .epochEnd(2000l)
                .partitionValue("foo")
                .build();

        assertEquals(20, query.getSkip());
        assertEquals(40, query.getTake());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(true, query.isExpand());
        assertEquals(50.0d, query.getAnomalyScoreFilter(), 0.00001);
        assertEquals(70.0d, query.getNormalizedProbability(), 0.00001);
        assertEquals(1000l, query.getEpochStart());
        assertEquals(2000l, query.getEpochEnd());
        assertEquals("foo", query.getPartitionValue());
    }

    @Test
    public void testEqualsHash()
    {
        BucketsQueryBuilder query = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart(1000l)
                .epochEnd(2000l)
                .partitionValue("foo");

        BucketsQueryBuilder query2 = new BucketsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .expand(true)
                .anomalyScoreThreshold(50.0d)
                .normalizedProbabilityThreshold(70.0d)
                .epochStart(1000l)
                .epochEnd(2000l)
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
        .epochStart(1000l)
        .epochEnd(2000l)
        .partitionValue("foo");
        assertEquals(query.build(), query2.build());

        query2.clear();
        query2.skip(20)
        .take(40)
        .includeInterim(true)
        .expand(true)
        .anomalyScoreThreshold(50.1d)
        .normalizedProbabilityThreshold(70.0d)
        .epochStart(1000l)
        .epochEnd(2000l)
        .partitionValue("foo");
        assertFalse(query.build().equals(query2.build()));
    }
}