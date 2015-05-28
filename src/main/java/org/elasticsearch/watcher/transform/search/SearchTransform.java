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

package org.elasticsearch.watcher.transform.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.SearchRequestEquivalence;
import org.elasticsearch.watcher.support.SearchRequestParseException;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class SearchTransform implements Transform {

    public static final String TYPE = "search";

    protected final SearchRequest request;

    public SearchTransform(SearchRequest request) {
        this.request = request;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public SearchRequest getRequest() {
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchTransform transform = (SearchTransform) o;

        return SearchRequestEquivalence.INSTANCE.equivalent(request, transform.request);
    }

    @Override
    public int hashCode() {
        return request.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return WatcherUtils.writeSearchRequest(request, builder, params);
    }

    public static SearchTransform parse(String watchId, XContentParser parser) throws IOException {
        try {
            SearchRequest request = WatcherUtils.readSearchRequest(parser, ExecutableSearchTransform.DEFAULT_SEARCH_TYPE);
            return new SearchTransform(request);
        } catch (SearchRequestParseException srpe) {
            throw new SearchTransformException("could not parse [{}] transform for watch [{}]. failed parsing search request", srpe, TYPE, watchId);
        }
    }

    public static Builder builder(SearchRequest request) {
        return new Builder(request);
    }

    public static class Result extends Transform.Result {

        private final SearchRequest request;

        public Result(SearchRequest request, Payload payload) {
            super(TYPE, payload);
            this.request = request;
        }

        public SearchRequest executedRequest() {
            return request;
        }

        @Override
        protected XContentBuilder xContentBody(XContentBuilder builder, Params params) throws IOException {
            builder.field(Field.REQUEST.getPreferredName());
            WatcherUtils.writeSearchRequest(request, builder, params);
            return builder;
        }
    }

    public static class Builder implements Transform.Builder<SearchTransform> {

        private final SearchRequest request;

        public Builder(SearchRequest request) {
            this.request = request;
        }

        @Override
        public SearchTransform build() {
            return new SearchTransform(request);
        }
    }

    public interface Field extends Transform.Field {
        ParseField REQUEST = new ParseField("request");
    }
}
