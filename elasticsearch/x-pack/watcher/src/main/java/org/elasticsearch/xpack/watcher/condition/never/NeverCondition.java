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

package org.elasticsearch.xpack.watcher.condition.never;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.Condition;

import java.io.IOException;

/**
 *
 */
public class NeverCondition implements Condition {

    public static final String TYPE = "never";
    public static final NeverCondition INSTANCE = new NeverCondition();

    @Override
    public final String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    public static NeverCondition parse(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an empty object but found [{}]",
                    TYPE, watchId, parser.currentName());
        }
        XContentParser.Token token = parser.nextToken();
        if (token != XContentParser.Token.END_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an empty object but found [{}]",
                    TYPE, watchId, parser.currentName());
        }
        return INSTANCE;
    }

    public static class Result extends Condition.Result {

        public static final Result INSTANCE = new Result();

        private Result() {
            super(TYPE, false);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            return builder;
        }
    }

    public static class Builder implements Condition.Builder<NeverCondition> {

        public static final Builder INSTANCE = new Builder();

        public NeverCondition build() {
            return NeverCondition.INSTANCE;
        }
    }
}
