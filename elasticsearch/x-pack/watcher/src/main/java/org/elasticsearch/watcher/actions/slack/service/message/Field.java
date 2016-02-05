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

package org.elasticsearch.watcher.actions.slack.service.message;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.slack.service.message.SlackMessageDefaults.AttachmentDefaults.FieldDefaults;
import org.elasticsearch.watcher.support.text.TextTemplate;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
class Field implements MessageElement {

    final String title;
    final String value;
    final boolean isShort;

    public Field(String title, String value, boolean isShort) {
        this.title = title;
        this.value = value;
        this.isShort = isShort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        if (isShort != field.isShort) return false;
        if (!title.equals(field.title)) return false;
        return value.equals(field.value);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (isShort ? 1 : 0);
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(XField.TITLE.getPreferredName(), title)
                .field(XField.VALUE.getPreferredName(), value)
                .field(XField.SHORT.getPreferredName(), isShort)
                .endObject();
    }

    static class Template implements ToXContent {

        final TextTemplate title;
        final TextTemplate value;
        final Boolean isShort;

        public Template(TextTemplate title, TextTemplate value, Boolean isShort) {
            this.title = title;
            this.value = value;
            this.isShort = isShort;
        }

        public Field render(TextTemplateEngine engine, Map<String, Object> model, FieldDefaults defaults) {
            String title = this.title != null ? engine.render(this.title, model) : defaults.title;
            String value = this.value != null ? engine.render(this.value, model) : defaults.value;
            Boolean isShort = this.isShort != null ? this.isShort : defaults.isShort;
            return new Field(title, value, isShort);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Template template = (Template) o;

            if (isShort != template.isShort) return false;
            if (!title.equals(template.title)) return false;
            return value.equals(template.value);
        }

        @Override
        public int hashCode() {
            int result = title.hashCode();
            result = 31 * result + value.hashCode();
            result = 31 * result + (isShort ? 1 : 0);
            return result;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                    .field(XField.TITLE.getPreferredName(), title)
                    .field(XField.VALUE.getPreferredName(), value)
                    .field(XField.SHORT.getPreferredName(), isShort)
                    .endObject();
        }

        public static Template parse(XContentParser parser) throws IOException {

            TextTemplate title = null;
            TextTemplate value = null;
            boolean isShort = false;

            XContentParser.Token token = null;
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, XField.TITLE)) {
                    try {
                        title = TextTemplate.parse(parser);
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse message attachment field. failed to parse [{}] field", pe,
                                XField.TITLE);
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, XField.VALUE)) {
                    try {
                        value = TextTemplate.parse(parser);
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse message attachment field. failed to parse [{}] field", pe,
                                XField.VALUE);
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, XField.SHORT)) {
                    if (token == XContentParser.Token.VALUE_BOOLEAN) {
                        isShort = parser.booleanValue();
                    } else {
                        throw new ElasticsearchParseException("could not parse message attachment field. expected a boolean value for " +
                                "[{}] field, but found [{}]", XField.SHORT, token);
                    }
                } else {
                    throw new ElasticsearchParseException("could not parse message attachment field. unexpected field [{}]",
                            currentFieldName);
                }
            }

            if (title == null) {
                throw new ElasticsearchParseException("could not parse message attachment field. missing required [{}] field",
                        XField.TITLE);
            }
            if (value == null) {
                throw new ElasticsearchParseException("could not parse message attachment field. missing required [{}] field",
                        XField.VALUE);
            }
            return new Template(title, value, isShort);
        }
    }

    interface XField extends MessageElement.XField {
        ParseField VALUE = new ParseField("value");
        ParseField SHORT = new ParseField("short");
    }
}
