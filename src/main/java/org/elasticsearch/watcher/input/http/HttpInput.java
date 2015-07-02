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

package org.elasticsearch.watcher.input.http;

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.support.http.HttpContentType;
import org.elasticsearch.watcher.support.http.HttpRequest;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class HttpInput implements Input {

    public static final String TYPE = "http";

    private final HttpRequestTemplate request;
    private final @Nullable HttpContentType expectedResponseXContentType;
    private final @Nullable Set<String> extractKeys;

    public HttpInput(HttpRequestTemplate request, @Nullable HttpContentType expectedResponseXContentType, @Nullable Set<String> extractKeys) {
        this.request = request;
        this.expectedResponseXContentType = expectedResponseXContentType;
        this.extractKeys = extractKeys;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public HttpRequestTemplate getRequest() {
        return request;
    }

    public Set<String> getExtractKeys() {
        return extractKeys;
    }

    public HttpContentType getExpectedResponseXContentType() {
        return expectedResponseXContentType;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Field.REQUEST.getPreferredName(), request, params);
        if (extractKeys != null) {
            builder.field(Field.EXTRACT.getPreferredName(), extractKeys);
        }
        if (expectedResponseXContentType != null) {
            builder.field(Field.RESPONSE_CONTENT_TYPE.getPreferredName(), expectedResponseXContentType, params);
        }
        builder.endObject();
        return builder;
    }

    public static HttpInput parse(String watchId, XContentParser parser, HttpRequestTemplate.Parser requestParser) throws IOException {
        Set<String> extract = null;
        HttpRequestTemplate request = null;
        HttpContentType expectedResponseBodyType = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.REQUEST)) {
                try {
                    request = requestParser.parse(parser);
                } catch (HttpRequestTemplate.ParseException pe) {
                    throw new HttpInputException("could not parse [{}] input for watch [{}]. failed to parse http request template", pe, TYPE, watchId);
                }
            } else if (token == XContentParser.Token.START_ARRAY) {
                if (Field.EXTRACT.getPreferredName().equals(currentFieldName)) {
                    extract = new HashSet<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        if (token == XContentParser.Token.VALUE_STRING) {
                            extract.add(parser.text());
                        } else {
                            throw new HttpInputException("could not parse [{}] input for watch [{}]. expected a string value as an [{}] item but found [{}] instead", TYPE, watchId, currentFieldName, token);
                        }
                    }
                } else {
                    throw new HttpInputException("could not parse [{}] input for watch [{}]. unexpected array field [{}]", TYPE, watchId, currentFieldName);
                }
            } else if (token == XContentParser.Token.VALUE_STRING) {
                if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.RESPONSE_CONTENT_TYPE)) {
                    expectedResponseBodyType = HttpContentType.resolve(parser.text());
                    if (expectedResponseBodyType == null) {
                        throw new HttpInputException("could not parse [{}] input for watch [{}]. unknown content type [{}]", TYPE, watchId, parser.text());
                    }
                } else {
                    throw new HttpInputException("could not parse [{}] input for watch [{}]. unexpected string field [{}]", TYPE, watchId, currentFieldName);
                }
            } else {
                throw new HttpInputException("could not parse [{}] input for watch [{}]. unexpected token [{}]", TYPE, watchId, token);
            }
        }

        if (request == null) {
            throw new HttpInputException("could not parse [{}] input for watch [{}]. missing require [{}] field", TYPE, watchId, Field.REQUEST.getPreferredName());
        }

        if (expectedResponseBodyType == HttpContentType.TEXT && extract != null ) {
            throw new HttpInputException("could not parse [{}] input for watch [{}]. key extraction is not supported for content type [{}]", TYPE, watchId, expectedResponseBodyType);
        }

        return new HttpInput(request, expectedResponseBodyType, extract);
    }

    public static Builder builder(HttpRequestTemplate httpRequest) {
        return new Builder(httpRequest);
    }

    public static class Result extends Input.Result {

        private final @Nullable HttpRequest request;
        private final int statusCode;

        public Result(HttpRequest request, int statusCode, Payload payload) {
            super(TYPE, payload);
            this.request = request;
            this.statusCode = statusCode;
        }

        public Result(@Nullable HttpRequest request, Exception e) {
            super(TYPE, e);
            this.request = request;
            this.statusCode = -1;
        }

        public HttpRequest request() {
            return request;
        }

        public int statusCode() {
            return statusCode;
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            if (request == null) {
                return builder;
            }
            builder.startObject(type);
            builder.field(Field.REQUEST.getPreferredName(), request, params);
            if (statusCode > 0) {
                builder.field(Field.STATUS_CODE.getPreferredName(), statusCode);
            }
            return builder.endObject();
        }
    }

    public static class Builder implements Input.Builder<HttpInput> {

        private final HttpRequestTemplate request;
        private final ImmutableSet.Builder<String> extractKeys = ImmutableSet.builder();
        private HttpContentType expectedResponseXContentType = null;

        private Builder(HttpRequestTemplate request) {
            this.request = request;
        }

        public Builder extractKeys(Collection<String> keys) {
            extractKeys.addAll(keys);
            return this;
        }

        public Builder extractKeys(String... keys) {
            extractKeys.add(keys);
            return this;
        }

        public Builder expectedResponseXContentType(HttpContentType expectedResponseXContentType) {
            this.expectedResponseXContentType = expectedResponseXContentType;
            return this;
        }

        @Override
        public HttpInput build() {
            ImmutableSet<String> keys = extractKeys.build();
            return new HttpInput(request, expectedResponseXContentType, keys.isEmpty() ? null : keys);
        }
    }

    interface Field extends Input.Field {
        ParseField REQUEST = new ParseField("request");
        ParseField EXTRACT = new ParseField("extract");
        ParseField STATUS_CODE = new ParseField("status_code");
        ParseField RESPONSE_CONTENT_TYPE = new ParseField("response_content_type");
    }
}
