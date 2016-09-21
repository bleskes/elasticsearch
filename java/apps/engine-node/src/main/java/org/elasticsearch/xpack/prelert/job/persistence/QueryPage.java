
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.List;

/**
 * Generic wrapper class for a page of query results and the
 * total number of query hits.<br>
 * {@linkplain #hitCount()} is the total number of results
 * but that value may not be equal to the actual length of
 * the {@linkplain #queryResults()} list if skip & take or
 * some cursor was used in the database query.
 *
 * @param <T>
 */
public final class QueryPage<T>
{
    private final List<T> queryResults;
    private final long hitCount;

    public QueryPage(List<T> queryResults, long hitCount)
    {
        this.queryResults = queryResults;
        this.hitCount = hitCount;
    }

    public List<T> queryResults()
    {
        return queryResults;
    }

    public long hitCount()
    {
        return hitCount;
    }
}
