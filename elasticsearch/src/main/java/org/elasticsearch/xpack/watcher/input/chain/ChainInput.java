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

package org.elasticsearch.xpack.watcher.input.chain;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.input.InputRegistry;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChainInput implements Input {

    public static final String TYPE = "chain";
    public static final ParseField INPUTS = new ParseField("inputs");

    private final List<Tuple<String, Input>> inputs;

    public ChainInput(List<Tuple<String, Input>> inputs) {
        this.inputs = inputs;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray(INPUTS.getPreferredName());
        for (Tuple<String, Input> tuple : inputs) {
            builder.startObject().startObject(tuple.v1());
            builder.field(tuple.v2().type(), tuple.v2());
            builder.endObject().endObject();
        }
        builder.endArray();
        builder.endObject();

        return builder;
    }

    public List<Tuple<String, Input>> getInputs() {
        return inputs;
    }

    public static ChainInput parse(String watchId, XContentParser parser, InputRegistry inputRegistry) throws IOException {
        List<Tuple<String, Input>> inputs = new ArrayList<>();
        String currentFieldName;
        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
                token = parser.nextToken();
                if (token == XContentParser.Token.START_ARRAY && INPUTS.getPreferredName().equals(currentFieldName)) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        if (token == XContentParser.Token.FIELD_NAME) {
                            String inputName = parser.currentName();
                            inputs.add(new Tuple<>(inputName, parseSingleInput(watchId, inputName, parser, inputRegistry)));
                        }
                    }
                }
            }
        }

        return new ChainInput(inputs);
    }

    private static Input parseSingleInput(String watchId, String name, XContentParser parser,
                                          InputRegistry inputRegistry) throws IOException {
        if (parser.nextToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("Expected starting JSON object after [{}] in watch [{}]", name, watchId);
        }

        Input input = inputRegistry.parse(watchId, parser).input();

        // expecting closing of two json object to start the next element in the array
        if (parser.currentToken() != XContentParser.Token.END_OBJECT || parser.nextToken() != XContentParser.Token.END_OBJECT) {
            throw new ElasticsearchParseException("Expected closing JSON object after parsing input [{}] named [{}] in watch [{}]",
                    input.type(), name, watchId);
        }

        return input;
    }

    public static ChainInput.Builder builder() {
        return new Builder();
    }

    public static class Builder implements Input.Builder<ChainInput> {

        private List<Tuple<String, Input>> inputs;

        private Builder() {
            inputs = new ArrayList<>();
        }

        public Builder add(String name, Input.Builder input) {
            inputs.add(new Tuple<>(name, input.build()));
            return this;
        }

        @Override
        public ChainInput build() {
            return new ChainInput(inputs);
        }
    }

    public static class Result extends Input.Result {

        private List<Tuple<String, Input.Result>> results = Collections.emptyList();

        protected Result(List<Tuple<String, Input.Result>> results, Payload payload) {
            super(TYPE, payload);
            this.results = results;
        }

        protected Result(Exception e) {
            super(TYPE, e);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(type);
            for (Tuple<String, Input.Result> tuple : results) {
                builder.field(tuple.v1(), tuple.v2());
            }
            builder.endObject();

            return builder;
        }
    }
}
