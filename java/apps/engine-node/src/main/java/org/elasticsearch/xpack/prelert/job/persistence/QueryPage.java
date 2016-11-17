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

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Generic wrapper class for a page of query results and the total number of
 * query hits.<br>
 * {@linkplain #hitCount()} is the total number of results but that value may
 * not be equal to the actual length of the {@linkplain #hits()} list if from
 * &amp; take or some cursor was used in the database query.
 */
public final class QueryPage<T extends ToXContent & Writeable> extends ToXContentToBytes implements Writeable {

    public static final ParseField HITS = new ParseField("hits");
    public static final ParseField HIT_COUNT = new ParseField("hitCount");

    private final List<T> hits;
    private final long hitCount;

    public QueryPage(List<T> hits, long hitCount) {
        this.hits = hits;
        this.hitCount = hitCount;
    }

    public QueryPage(StreamInput in, Reader<T> hitReader) throws IOException {
        hits = in.readList(hitReader);
        hitCount = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(hits);
        out.writeLong(hitCount);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        doXContentBody(builder, params);
        builder.endObject();
        return builder;
    }

    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(HITS.getPreferredName(), hits);
        builder.field(HIT_COUNT.getPreferredName(), hitCount);
        return builder;
    }

    public List<T> hits() {
        return hits;
    }

    public long hitCount() {
        return hitCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hits, hitCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        QueryPage<T> other = (QueryPage<T>) obj;
        return Objects.equals(hits, other.hits) &&
                Objects.equals(hitCount, other.hitCount);
    }
}
