/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

public class BucketQueryBuilderTest extends ESTestCase {

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
