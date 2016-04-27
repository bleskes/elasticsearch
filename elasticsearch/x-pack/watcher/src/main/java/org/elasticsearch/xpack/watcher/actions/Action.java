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

package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 *
 */
public interface Action extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        public enum Status implements ToXContent {
            SUCCESS,
            FAILURE,
            PARTIAL_FAILURE,
            THROTTLED,
            SIMULATED;

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.value(name().toLowerCase(Locale.ROOT));
            }
        }

        protected final String type;
        protected final Status status;

        protected Result(String type, Status status) {
            this.type = type;
            this.status = status;
        }

        public String type() {
            return type;
        }

        public Status status() {
            return status;
        }

        public static class Failure extends Result {

            private final String reason;

            public Failure(String type, String reason, Object... args) {
                super(type, Status.FAILURE);
                this.reason = LoggerMessageFormat.format(reason, args);
            }

            public String reason() {
                return reason;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.field(Field.REASON.getPreferredName(), reason);
            }
        }

        public static class Throttled extends Result {

            private final String reason;

            public Throttled(String type, String reason) {
                super(type, Status.THROTTLED);
                this.reason = reason;
            }

            public String reason() {
                return reason;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.field(Field.REASON.getPreferredName(), reason);
            }
        }
    }

    interface Builder<A extends Action> {

        A build();
    }

    interface Field {
        ParseField REASON = new ParseField("reason");
    }
}
