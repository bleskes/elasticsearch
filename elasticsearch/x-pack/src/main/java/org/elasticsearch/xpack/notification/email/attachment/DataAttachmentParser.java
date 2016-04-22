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

package org.elasticsearch.xpack.notification.email.attachment;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.xpack.notification.email.Attachment;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.xpack.notification.email.DataAttachment.resolve;

public class DataAttachmentParser implements EmailAttachmentParser<DataAttachment> {

    interface Fields {
        ParseField FORMAT = new ParseField("format");
    }

    public static final String TYPE = "data";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public DataAttachment parse(String id, XContentParser parser) throws IOException {
        org.elasticsearch.xpack.notification.email.DataAttachment dataAttachment =
                org.elasticsearch.xpack.notification.email.DataAttachment.YAML;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (Strings.hasLength(currentFieldName) && ParseFieldMatcher.STRICT.match(currentFieldName, Fields.FORMAT)) {
                if (token == XContentParser.Token.VALUE_STRING) {
                    dataAttachment = resolve(parser.text());
                } else {
                    throw new ElasticsearchParseException("could not parse data attachment. expected string value for [{}] field but " +
                            "found [{}] instead", currentFieldName, token);
                }
            }
        }

        return new DataAttachment(id, dataAttachment);
    }

    @Override
    public Attachment toAttachment(WatchExecutionContext ctx, Payload payload, DataAttachment attachment) {
        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        return attachment.getDataAttachment().create(attachment.id(), model);
    }
}
