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

import org.junit.Test;

import static org.junit.Assert.*;

public class RecordsQueryBuilderTest
{
    @Test
    public void testDefaults() throws Exception
    {
        RecordsQueryBuilder.RecordsQuery query = new RecordsQueryBuilder().build();

        assertEquals(0, query.getSkip());
        assertEquals(RecordsQueryBuilder.DEFAULT_TAKE_SIZE, query.getTake());
        assertEquals(false, query.isIncludeInterim());
        assertEquals(true, query.isSortDescending());
        assertNull(query.getSortField());
        assertEquals(0.0, query.getAnomalyScoreThreshold(), 0.0001);
        assertEquals(0.0, query.getNormalizedProbabilityThreshold(), 0.0001);
        assertNull(query.getPartitionFieldValue());
        assertEquals(-1l, query.getEpochStart());
        assertEquals(-1l, query.getEpochEnd());
    }

    @Test
    public void testAll() {
        RecordsQueryBuilder.RecordsQuery query = new RecordsQueryBuilder()
                .skip(20)
                .take(40)
                .includeInterim(true)
                .sortField("score")
                .sortDescending(false)
                .anomalyScoreFilter(50.0d)
                .normalizedProbability(70.0d)
                .partitionFieldValue("p")
                .epochStart(500l)
                .epochEnd(1500l)
                .build();

        assertEquals(20, query.getSkip());
        assertEquals(40, query.getTake());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(false, query.isSortDescending());
        assertEquals("score", query.getSortField());
        assertEquals(50.0d, query.getAnomalyScoreThreshold(), 0.00001);
        assertEquals(70.0d, query.getNormalizedProbabilityThreshold(), 0.00001);
        assertEquals("p", query.getPartitionFieldValue());
        assertEquals(500l, query.getEpochStart());
        assertEquals(1500l, query.getEpochEnd());
    }
}