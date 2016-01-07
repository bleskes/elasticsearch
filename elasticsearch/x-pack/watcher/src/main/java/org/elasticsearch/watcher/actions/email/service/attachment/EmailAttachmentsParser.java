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

package org.elasticsearch.watcher.actions.email.service.attachment;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmailAttachmentsParser {

    private Map<String, EmailAttachmentParser> parsers;

    @Inject
    public EmailAttachmentsParser(Map<String, EmailAttachmentParser> parsers) {
        this.parsers = parsers;
    }

    public EmailAttachments parse(XContentParser parser) throws IOException {
        List<EmailAttachmentParser.EmailAttachment> attachments = new ArrayList<>();
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else {
                if (token == XContentParser.Token.START_OBJECT && currentFieldName != null) {
                    String currentAttachmentType = null;
                    if (parser.nextToken() == XContentParser.Token.FIELD_NAME) {
                        currentAttachmentType = parser.currentName();
                    }
                    parser.nextToken();

                    EmailAttachmentParser emailAttachmentParser = parsers.get(currentAttachmentType);
                    if (emailAttachmentParser == null) {
                        throw new ElasticsearchParseException("Cannot parse attachment of type " + currentAttachmentType);
                    }
                    EmailAttachmentParser.EmailAttachment emailAttachment = emailAttachmentParser.parse(currentFieldName, parser);
                    attachments.add(emailAttachment);
                    // one further to skip the end_object from the attachment
                    parser.nextToken();
                }
            }
        }

        return new EmailAttachments(attachments);
    }

}
