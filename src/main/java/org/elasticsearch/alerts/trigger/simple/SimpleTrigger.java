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

package org.elasticsearch.alerts.trigger.simple;

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * A trigger that always triggered and returns a static/fixed data
 */
public class SimpleTrigger extends Trigger<SimpleTrigger.Result> {

    public static final String TYPE = "simple";

    private final Payload payload;

    public SimpleTrigger(ESLogger logger, Payload payload) {
        super(logger);
        this.payload = payload;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Result execute(Alert alert, DateTime scheduledFireTime, DateTime fireTime) throws IOException {
        return new Result(payload);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return payload.toXContent(builder, params);
    }

    public static class Result extends Trigger.Result {

        public Result(Payload payload) {
            super(TYPE, true, payload);
        }
    }

    public static class Parser extends AbstractComponent implements Trigger.Parser<SimpleTrigger> {

        @Inject
        public Parser(Settings settings) {
            super(settings);
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public SimpleTrigger parse(XContentParser parser) throws IOException {
            return new SimpleTrigger(logger, new Payload.XContent(parser));
        }
    }
}
