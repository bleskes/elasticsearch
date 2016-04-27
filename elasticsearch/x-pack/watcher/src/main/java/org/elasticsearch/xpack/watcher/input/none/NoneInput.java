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

package org.elasticsearch.xpack.watcher.input.none;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class NoneInput implements Input {

    public static final String TYPE = "none";
    public static final NoneInput INSTANCE = new NoneInput();

    private NoneInput() {
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    public static NoneInput parse(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            String formattedMessage = "could not parse [{}] input for watch [{}]. expected an empty object but found [{}] instead";
            throw new ElasticsearchParseException(formattedMessage, TYPE, watchId, parser.currentToken());
        }
        if (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            String formattedMessage = "could not parse [{}] input for watch [{}]. expected an empty object but found [{}] instead";
            throw new ElasticsearchParseException(formattedMessage, TYPE, watchId, parser.currentToken());
        }
        return INSTANCE;
    }

    public static Builder builder() {
        return Builder.INSTANCE;
    }

    public static class Result extends Input.Result {

        static final Result INSTANCE = new Result();

        private Result() {
            super(TYPE, Payload.EMPTY);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            return builder;
        }
    }

    public static class Builder implements Input.Builder<NoneInput> {

        private static final Builder INSTANCE = new Builder();

        private Builder() {
        }

        @Override
        public NoneInput build() {
            return NoneInput.INSTANCE;
        }
    }
}
