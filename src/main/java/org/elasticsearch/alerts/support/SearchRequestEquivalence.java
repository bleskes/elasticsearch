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

package org.elasticsearch.alerts.support;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.AlertsException;
import org.elasticsearch.common.base.Equivalence;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * The only true way today to compare search request object (outside of core) is to
 * serialize it and compare the serialized output. this is heavy obviously, but luckily we
 * don't compare search requests in normal runtime... we only do it in the tests. The is here basically
 * due to the lack of equals/hashcode support in SearchRequest in core.
 */
public final class SearchRequestEquivalence extends Equivalence<SearchRequest> {

    public static final SearchRequestEquivalence INSTANCE = new SearchRequestEquivalence();

    private SearchRequestEquivalence() {
    }

    @Override
    protected boolean doEquivalent(SearchRequest r1, SearchRequest r2) {
        try {
            BytesStreamOutput output1 = new BytesStreamOutput();
            r1.writeTo(output1);
            BytesReference bytes1 = output1.bytes();
            BytesStreamOutput output2 = new BytesStreamOutput(bytes1.length());
            r2.writeTo(output2);
            BytesReference bytes2 = output2.bytes();
            return Arrays.equals(bytes1.toBytes(), bytes2.toBytes());
        } catch (IOException ioe) {
            throw new AlertsException("could not compare search requests", ioe);
        }
    }

    @Override
    protected int doHash(SearchRequest request) {
        try {
            BytesStreamOutput output = new BytesStreamOutput();
            request.writeTo(output);
            return Arrays.hashCode(output.bytes().toBytes());
        } catch (IOException ioe) {
            throw new AlertsException("could not compute hashcode for search request", ioe);
        }
    }
}
