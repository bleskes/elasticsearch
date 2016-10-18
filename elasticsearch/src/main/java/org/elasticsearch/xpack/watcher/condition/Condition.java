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

package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 *
 */
public interface Condition extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        public enum Status {
            SUCCESS, FAILURE
        }

        protected final String type;
        protected final Status status;
        private final String reason;
        protected final boolean met;

        public Result(String type, boolean met) {
            // TODO: FAILURE status is never used, but a some code assumes that it is used
            this.status = Status.SUCCESS;
            this.type = type;
            this.met = met;
            this.reason = null;
        }

        public String type() {
            return type;
        }

        public Status status() {
            return status;
        }

        public boolean met() {
            return met;
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
                    builder.field(Field.MET.getPreferredName(), met);
                    break;
                case FAILURE:
                    assert reason != null && !met;
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

    interface Field {
        ParseField TYPE = new ParseField("type");
        ParseField STATUS = new ParseField("status");
        ParseField MET = new ParseField("met");
        ParseField REASON = new ParseField("reason");
    }
}
