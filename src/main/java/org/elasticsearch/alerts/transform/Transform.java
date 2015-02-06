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

package org.elasticsearch.alerts.transform;

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public interface Transform extends ToXContent {

    static final Transform NOOP = new Transform() {
        @Override
        public String type() {
            return "noop";
        }

        @Override
        public Payload apply(Alert alert, Trigger.Result result, Payload payload, DateTime scheduledFireTime, DateTime fireTime) throws IOException {
            return payload;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().endObject();
        }
    };

    String type();

    Payload apply(Alert alert, Trigger.Result result, Payload payload, DateTime scheduledFireTime, DateTime fireTime) throws IOException;

    static interface Parser<P extends Transform> {

        String type();

        P parse(XContentParser parser) throws IOException;

    }

}
