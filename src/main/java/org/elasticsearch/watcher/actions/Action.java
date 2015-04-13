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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 *
 */
public interface Action extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        protected final String type;
        protected final boolean success;

        protected Result(String type, boolean success) {
            this.type = type;
            this.success = success;
        }

        public String type() {
            return type;
        }

        public boolean success() {
            return success;
        }

        @Override
        public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Field.SUCCESS.getPreferredName(), success);
            xContentBody(builder, params);
            return builder.endObject();
        }

        protected abstract XContentBuilder xContentBody(XContentBuilder builder, Params params) throws IOException;

        public interface Failure extends ToXContent {

            String reason();

        }
    }

    interface Builder<A extends Action> {

        A build();
    }

    interface Field {
        ParseField SUCCESS = new ParseField("success");
        ParseField REASON = new ParseField("reason");
    }
}
