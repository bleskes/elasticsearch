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

package org.elasticsearch.alerts.trigger;

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public abstract class Trigger<R extends Trigger.Result> implements ToXContent {

    protected final ESLogger logger;

    protected Trigger(ESLogger logger) {
        this.logger = logger;
    }

    /**
     * @return the type of this trigger
     */
    public abstract String type();

    /**
     * Executes this trigger
     */
    public abstract R execute(Alert alert, DateTime scheduledFireTime, DateTime fireTime) throws IOException;


    /**
     * Parses xcontent to a concrete trigger of the same type.
     */
    public static interface Parser<T extends Trigger> {

        /**
         * @return  The type of the trigger
         */
        String type();

        /**
         * Parses the given xcontent and creates a concrete trigger
         */
        T parse(XContentParser parser) throws IOException;
    }

    public static abstract class Result {

        private final String type;
        private final boolean triggered;
        private final Payload payload;

        public Result(String type, boolean triggered, Payload payload) {
            this.type = type;
            this.triggered = triggered;
            this.payload = payload;
        }

        public String type() {
            return type;
        }

        public boolean triggered() {
            return triggered;
        }

        public Payload payload() {
            return payload;
        }

    }
}
