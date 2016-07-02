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

package org.elasticsearch.xpack.watcher.support;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;

import java.util.Arrays;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;


/**
 * The only true way today to compare search request object (outside of core) is to
 * serialize it and compare the serialized output. this is heavy obviously, but luckily we
 * don't compare search requests in normal runtime... we only do it in the tests. The is here basically
 * due to the lack of equals/hashcode support in SearchRequest in core.
 */
public final class SearchRequestEquivalence {

    public static final SearchRequestEquivalence INSTANCE = new SearchRequestEquivalence();

    private SearchRequestEquivalence() {
    }

    public final boolean equivalent(@Nullable SearchRequest a, @Nullable SearchRequest b) {
        return a == b ? true : (a != null && b != null ? this.doEquivalent(a, b) : false);
    }

    protected boolean doEquivalent(SearchRequest r1, SearchRequest r2) {
        try {
            BytesStreamOutput output1 = new BytesStreamOutput();
            r1.writeTo(output1);
            byte[] bytes1 = BytesReference.toBytes(output1.bytes());
            output1.reset();
            r2.writeTo(output1);
            byte[] bytes2 = BytesReference.toBytes(output1.bytes());
            return Arrays.equals(bytes1, bytes2);
        } catch (Throwable t) {
            throw illegalState("could not compare search requests", t);
        }
    }
}
