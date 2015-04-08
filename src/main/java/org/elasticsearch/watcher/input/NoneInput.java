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

package org.elasticsearch.watcher.input;


import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class NoneInput extends Input<NoneInput.Result> {

    public static final String TYPE = "none";

    private static final Payload EMPTY_PAYLOAD = new Payload() {
        @Override
        public Map<String, Object> data() {
            return ImmutableMap.of();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().endObject();
        }
    };

    public NoneInput(ESLogger logger) {
        super(logger);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Result execute(WatchExecutionContext ctx) throws IOException {
        return Result.INSTANCE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    public static class Result extends Input.Result {

        static final Result INSTANCE = new Result();

        private Result() {
            super(TYPE, EMPTY_PAYLOAD);
        }

        @Override
        protected XContentBuilder toXContentBody(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().endObject();
        }
    }

    public static class Parser extends AbstractComponent implements Input.Parser<Result, NoneInput> {

        private final NoneInput input;

        @Inject
        public Parser(Settings settings) {
            super(settings);
            this.input = new NoneInput(logger);
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public NoneInput parse(XContentParser parser) throws IOException {
            if (parser.currentToken() != XContentParser.Token.START_OBJECT) {

            }
            parser.nextToken();
            if (parser.currentToken() != XContentParser.Token.END_OBJECT) {

            }
            return input;
        }

        @Override
        public Result parseResult(XContentParser parser) throws IOException {
            if (parser.currentToken() != XContentParser.Token.START_OBJECT) {

            }
            parser.nextToken();
            if (parser.currentToken() != XContentParser.Token.END_OBJECT) {

            }
            return Result.INSTANCE;
        }
    }

    public static class SourceBuilder implements Input.SourceBuilder {

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
