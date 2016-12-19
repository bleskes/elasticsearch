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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

public class BucketQueryBuilderTests extends ESTestCase {

    public void testDefaultBuild() throws Exception {
        BucketQueryBuilder.BucketQuery query = new BucketQueryBuilder("1000").build();

        Assert.assertEquals("1000", query.getTimestamp());
        assertEquals(false, query.isIncludeInterim());
        assertEquals(false, query.isExpand());
        assertEquals(null, query.getPartitionValue());
    }

    public void testDefaultAll() throws Exception {
        BucketQueryBuilder.BucketQuery query =
                new BucketQueryBuilder("1000")
                .expand(true)
                .includeInterim(true)
                .partitionValue("p")
                .build();

        Assert.assertEquals("1000", query.getTimestamp());
        assertEquals(true, query.isIncludeInterim());
        assertEquals(true, query.isExpand());
        assertEquals("p", query.getPartitionValue());
    }

    public void testEqualsHash() throws Exception {
        BucketQueryBuilder.BucketQuery query =
                new BucketQueryBuilder("1000")
                .expand(true)
                .includeInterim(true)
                .partitionValue("p")
                .build();

        BucketQueryBuilder.BucketQuery query2 =
                new BucketQueryBuilder("1000")
                .expand(true)
                .includeInterim(true)
                .partitionValue("p")
                .build();

        assertEquals(query2, query);
        assertEquals(query2.hashCode(), query.hashCode());

        query2 =
                new BucketQueryBuilder("1000")
                .expand(true)
                .includeInterim(true)
                .partitionValue("q")
                .build();

        assertFalse(query2.equals(query));
        assertFalse(query2.hashCode() == query.hashCode());
    }
}
