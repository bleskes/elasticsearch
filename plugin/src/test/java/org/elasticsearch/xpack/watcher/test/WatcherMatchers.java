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

package org.elasticsearch.xpack.watcher.test;

import org.elasticsearch.action.index.IndexRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.Matchers;

/**
 *
 */
public final class WatcherMatchers {

    private WatcherMatchers() {
    }

    public static IndexRequest indexRequest(String index, String type, String id) {
        return Matchers.argThat(indexRequestMatcher(index, type, id));
    }

    public static IndexRequest indexRequest(String index, String type, String id, IndexRequest.OpType opType) {
        return Matchers.argThat(indexRequestMatcher(index, type, id).opType(opType));
    }

    public static IndexRequest indexRequest(String index, String type, String id, Long version, IndexRequest.OpType opType) {
        return Matchers.argThat(indexRequestMatcher(index, type, id).version(version).opType(opType));
    }

    public static IndexRequestMatcher indexRequestMatcher(String index, String type, String id) {
        return new IndexRequestMatcher(index, type, id);
    }

    public static class IndexRequestMatcher extends TypeSafeMatcher<IndexRequest> {

        private final String index;
        private final String type;
        private final String id;
        private Long version;
        private IndexRequest.OpType opType;

        private IndexRequestMatcher(String index, String type, String id) {
            this.index = index;
            this.type = type;
            this.id = id;
        }

        public IndexRequestMatcher version(long version) {
            this.version = version;
            return this;
        }

        public IndexRequestMatcher opType(IndexRequest.OpType opType) {
            this.opType = opType;
            return this;
        }

        @Override
        protected boolean matchesSafely(IndexRequest request) {
            if (!index.equals(request.index()) || !type.equals(request.type()) || !id.equals(request.id())) {
                return false;
            }
            if (version != null && !version.equals(request.version())) {
                return false;
            }
            if (opType != null && !opType.equals(request.opType())) {
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is index request [" + index + "/" + type + "/" + id + "]");
        }
    }
}
