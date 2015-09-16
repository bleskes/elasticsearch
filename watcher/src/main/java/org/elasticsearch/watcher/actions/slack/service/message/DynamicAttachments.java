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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.slack.service.message.SlackMessageDefaults.AttachmentDefaults;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;
import org.elasticsearch.watcher.support.xcontent.ObjectPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DynamicAttachments implements MessageElement {

    private String listPath;
    private Attachment.Template attachment;

    public DynamicAttachments(String listPath, Attachment.Template attachment) {
        this.listPath = listPath;
        this.attachment = attachment;
    }

    public List<Attachment> render(TextTemplateEngine engine, Map<String, Object> model, AttachmentDefaults defaults) {
        Object value = ObjectPath.eval(listPath, model);
        if (!(value instanceof Iterable)) {
            throw new IllegalArgumentException("dynamic attachment could not be resolved. expected context [" + listPath + "] to be a list, but found [" + value + "] instead");
        }
        List<Attachment> attachments = new ArrayList<>();
        for (Object obj : (Iterable) value) {
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException("dynamic attachment could not be resolved. expected [" + listPath + "] list to contain key/value pairs, but found [" + obj + "] instead");
            }
            Map<String, Object> attachmentModel = (Map<String, Object>) obj;
            attachments.add(attachment.render(engine, attachmentModel, defaults));
        }
        return attachments;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(XField.LIST_PATH.getPreferredName(), listPath)
                .field(XField.TEMPLATE.getPreferredName(), attachment, params)
                .endObject();
    }

    public static DynamicAttachments parse(XContentParser parser) throws IOException {
        String listPath = null;
        Attachment.Template template = null;

        String currentFieldName = null;
        XContentParser.Token token = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, XField.LIST_PATH)) {
                if (token == XContentParser.Token.VALUE_STRING) {
                    listPath = parser.text();
                } else {
                    throw new ElasticsearchParseException("could not parse dynamic attachments. expected a string value for [{}] field, but found [{}]", XField.LIST_PATH.getPreferredName(), token);
                }
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, XField.TEMPLATE)) {
                try {
                    template = Attachment.Template.parse(parser);
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse dynamic attachments. failed to parse [{}] field", pe, XField.TEMPLATE.getPreferredName());
                }
            } else {
                throw new ElasticsearchParseException("could not parse dynamic attachments. unexpected field [{}]", currentFieldName);
            }
        }
        if (listPath == null) {
            throw new ElasticsearchParseException("could not parse dynamic attachments. missing required field [{}]", XField.LIST_PATH.getPreferredName());
        }
        if (template == null) {
            throw new ElasticsearchParseException("could not parse dynamic attachments. missing required field [{}]", XField.TEMPLATE.getPreferredName());
        }
        return new DynamicAttachments(listPath, template);
    }

    interface XField extends MessageElement.XField {
        ParseField LIST_PATH = new ParseField("list_path");
        ParseField TEMPLATE = new ParseField("attachment_template");
    }
}
