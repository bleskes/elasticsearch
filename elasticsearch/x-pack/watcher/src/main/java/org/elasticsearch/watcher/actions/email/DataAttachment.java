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

package org.elasticsearch.watcher.actions.email;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.watcher.actions.email.service.Attachment;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static org.elasticsearch.watcher.support.Exceptions.illegalArgument;

/**
 *
 */
public enum DataAttachment implements ToXContent {

    YAML() {
        @Override
        public String contentType() {
            return XContentType.YAML.mediaType();
        }

        @Override
        public Attachment create(Map<String, Object> data) {
            return new Attachment.XContent.Yaml("data", "data.yml", new Payload.Simple(data));
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().field(Field.FORMAT.getPreferredName(), "yaml").endObject();
        }
    },

    JSON() {
        @Override
        public String contentType() {
            return XContentType.JSON.mediaType();
        }

        @Override
        public Attachment create(Map<String, Object> data) {
            return new Attachment.XContent.Json("data", "data.json", new Payload.Simple(data));
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().field(Field.FORMAT.getPreferredName(), "json").endObject();
        }
    };

    static DataAttachment DEFAULT = YAML;

    public abstract String contentType();

    public abstract Attachment create(Map<String, Object> data);

    public static DataAttachment resolve(String format) {
        switch (format.toLowerCase(Locale.ROOT)) {
            case "yaml": return YAML;
            case "json": return JSON;
            default:
                throw illegalArgument("unknown data attachment format [{}]", format);
        }
    }

    public static DataAttachment parse(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_NULL) {
            return null;
        }
        if (token == XContentParser.Token.VALUE_BOOLEAN) {
            return parser.booleanValue() ? DEFAULT : null;
        }
        if (token != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse data attachment. expected either a boolean value or an object but found [{}] instead", token);
        }

        DataAttachment dataAttachment = DEFAULT;

        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (currentFieldName == null) {
                throw new ElasticsearchParseException("could not parse data attachment. expected [{}] field but found [{}] instead", Field.FORMAT.getPreferredName(), token);
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.FORMAT)) {
                if (token == XContentParser.Token.VALUE_STRING) {
                    dataAttachment = resolve(parser.text());
                } else {
                    throw new ElasticsearchParseException("could not parse data attachment. expected string value for [{}] field but found [{}] instead", currentFieldName, token);
                }
            } else {
                throw new ElasticsearchParseException("could not parse data attachment. unexpected field [{}]", currentFieldName);
            }
        }

        return dataAttachment;
    }

    interface Field {
        ParseField FORMAT = new ParseField("format");
    }
}
