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

package org.elasticsearch.watcher.condition.simple;

import org.elasticsearch.watcher.watch.WatchExecutionContext;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.condition.ConditionException;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 */
public class AlwaysTrueCondition extends Condition<Condition.Result> {

    public static final String TYPE = "always_true";

    public static final Result RESULT = new Result(TYPE, true) {

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().endObject();
        }
    };

    public AlwaysTrueCondition(ESLogger logger) {
        super(logger);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Result execute(WatchExecutionContext ctx) throws IOException {
        return RESULT;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AlwaysTrueCondition;
    }

    public static class Parser extends AbstractComponent implements Condition.Parser<Result, AlwaysTrueCondition> {

        @Inject
        public Parser(Settings settings) {
            super(settings);
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public AlwaysTrueCondition parse(XContentParser parser) throws IOException {
            if (parser.currentToken() != XContentParser.Token.START_OBJECT){
                throw new ConditionException("unable to parse [" + TYPE + "] condition. expected a start object token, found [" + parser.currentToken() + "]");
            }
            XContentParser.Token token = parser.nextToken();
            if (token != XContentParser.Token.END_OBJECT) {
                throw new ConditionException("unable to parse [" + TYPE + "] condition. expected an empty object, but found an object with [" + token + "]");
            }
            return new AlwaysTrueCondition(logger);
        }

        @Override
        public Result parseResult(XContentParser parser) throws IOException {
            if (parser.currentToken() != XContentParser.Token.START_OBJECT){
                throw new ConditionException("unable to parse [" + TYPE + "] condition result. expected a start object token, found [" + parser.currentToken() + "]");
            }
            XContentParser.Token token = parser.nextToken();
            if (token != XContentParser.Token.END_OBJECT) {
                throw new ConditionException("unable to parse [" + TYPE + "] condition. expected an empty object, but found an object with [" + token + "]");
            }
            return RESULT;
        }
    }

    public static class SourceBuilder implements Condition.SourceBuilder {

        public static final SourceBuilder INSTANCE = new SourceBuilder();

        private SourceBuilder() {
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().endObject();
        }
    }
}
