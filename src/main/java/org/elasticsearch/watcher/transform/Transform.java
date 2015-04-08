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

package org.elasticsearch.watcher.transform;

import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public abstract class Transform<R extends Transform.Result> implements ToXContent {

    public abstract String type();

    public abstract Result apply(WatchExecutionContext ctx, Payload payload) throws IOException;

    public static abstract class Result implements ToXContent {

        protected final String type;
        protected final Payload payload;

        public Result(String type, Payload payload) {
            this.type = type;
            this.payload = payload;
        }

        public String type() {
            return type;
        }

        public Payload payload() {
            return payload;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Parser.PAYLOAD_FIELD.getPreferredName(), payload);
            xContentBody(builder, params);
            return builder.endObject();
        }

        protected abstract XContentBuilder xContentBody(XContentBuilder builder, Params params) throws IOException;

    }

    public static interface Parser<R extends Transform.Result, T extends Transform<R>> {

        public static final ParseField PAYLOAD_FIELD = new ParseField("payload");
        public static final ParseField TRANSFORM_FIELD = new ParseField("transform");
        public static final ParseField TRANSFORM_RESULT_FIELD = new ParseField("transform_result");

        String type();

        T parse(XContentParser parser) throws IOException;

        R parseResult(XContentParser parser) throws IOException;

    }

    public static interface SourceBuilder extends ToXContent {

        String type();
    }

}
