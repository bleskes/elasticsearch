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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Generic wrapper class for a page of query results and the
 * total number of query hits.<br>
 * {@linkplain #hitCount()} is the total number of results
 * but that value may not be equal to the actual length of
 * the {@linkplain #hits()} list if skip & take or
 * some cursor was used in the database query.
 *
 * @param <T>
 */
@JsonPropertyOrder({"hitCount", "hits"})
public final class QueryPage<T> {

    private final List<T> hits;
    private final long hitCount;

    public QueryPage(List<T> hits, long hitCount) {
        this.hits = hits;
        this.hitCount = hitCount;
    }

    @JsonGetter("hits")
    public List<T> hits() {
        return hits;
    }

    @JsonGetter("hitCount")
    public long hitCount() {
        return hitCount;
    }
}
