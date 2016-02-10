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

package org.elasticsearch.watcher.input.simple;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class SimpleInput implements Input {

    public static final String TYPE = "simple";

    private final Payload payload;

    public SimpleInput(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleInput that = (SimpleInput) o;

        return payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return payload.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return payload.toXContent(builder, params);
    }

    public static SimpleInput parse(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] input for watch [{}]. expected an object but found [{}] instead",
                    TYPE, watchId, parser.currentToken());
        }
        Payload payload = new Payload.Simple(parser.map());
        return new SimpleInput(payload);
    }

    public static Builder builder(Payload payload) {
        return new Builder(payload);
    }

    public static class Result extends Input.Result {

        public Result(Payload payload) {
            super(TYPE, payload);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            return builder;
        }
    }

    public static class Builder implements Input.Builder<SimpleInput> {

        private final Payload payload;

        private Builder(Payload payload) {
            this.payload = payload;
        }

        @Override
        public SimpleInput build() {
            return new SimpleInput(payload);
        }
    }
}
