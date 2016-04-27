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

package org.elasticsearch.xpack.watcher.transform;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;
import java.util.Locale;

/**
 *
 */
public interface Transform extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        public enum Status {
            SUCCESS, FAILURE
        }

        protected final String type;
        protected final Status status;
        protected final @Nullable Payload payload;
        protected final @Nullable String reason;

        public Result(String type, Payload payload) {
            this.type = type;
            this.status = Status.SUCCESS;
            this.payload = payload;
            this.reason = null;
        }

        public Result(String type, Exception e) {
            this(type, ExceptionsHelper.detailedMessage(e));
        }

        public Result(String type, String errorMessage) {
            this.type = type;
            this.status = Status.FAILURE;
            this.reason = errorMessage;
            this.payload = null;
        }

        public String type() {
            return type;
        }

        public Status status() {
            return status;
        }

        public Payload payload() {
            assert status == Status.SUCCESS;
            return payload;
        }

        public String reason() {
            assert status == Status.FAILURE;
            return reason;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(Field.TYPE.getPreferredName(), type);
            builder.field(Field.STATUS.getPreferredName(), status.name().toLowerCase(Locale.ROOT));
            switch (status) {
                case SUCCESS:
                    assert reason == null;
                    builder.field(Field.PAYLOAD.getPreferredName(), payload, params);
                    break;
                case FAILURE:
                    assert payload == null;
                    builder.field(Field.REASON.getPreferredName(), reason);
                    break;
                default:
                    assert false;
            }
            typeXContent(builder, params);
            return builder.endObject();
        }

        protected abstract XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException;

    }

    interface Builder<T extends Transform> {

        T build();
    }

    interface Field {
        ParseField TRANSFORM = new ParseField("transform");

        ParseField TYPE = new ParseField("type");
        ParseField STATUS = new ParseField("status");
        ParseField PAYLOAD = new ParseField("payload");
        ParseField REASON = new ParseField("reason");

    }
}
