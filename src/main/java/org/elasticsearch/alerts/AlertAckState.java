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

package org.elasticsearch.alerts;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * @TODO add jdocs
 */
public enum AlertAckState implements ToXContent {
    NOT_ACKABLE, ///@TODO perhaps null
    NEEDS_ACK,
    ACKED,
    NOT_TRIGGERED;

    public static final String FIELD_NAME = "ack_state";

    @Override
    public String toString() {
        switch (this) {
            case NOT_ACKABLE:
                return "NOT_ACKABLE";
            case NEEDS_ACK:
                return "NEEDS_ACK";
            case ACKED:
                return "ACKED";
            case NOT_TRIGGERED:
                return "NOT_TRIGGERED";
            default:
                return "NOT_ACKABLE";
        }
    }

    public static AlertAckState fromString(String s) {
        switch (s.toUpperCase()) {
            case "NOT_ACKABLE":
                return NOT_ACKABLE;
            case "NEEDS_ACK":
                return NEEDS_ACK;
            case "ACKED":
                return ACKED;
            case "NOT_TRIGGERED":
                return NOT_TRIGGERED;
            default:
                return NOT_ACKABLE;
        }
    }

        @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_NAME);
        builder.value(this.toString());
        builder.endObject();
        return builder;
    }
}
