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

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

public class EmailAttachments implements ToXContent {

    public interface Fields {
        ParseField ATTACHMENTS = new ParseField("attachments");
    }

    private final List<EmailAttachmentParser.EmailAttachment> attachments;

    public EmailAttachments(List<EmailAttachmentParser.EmailAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<EmailAttachmentParser.EmailAttachment> getAttachments() {
        return attachments;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (attachments != null && attachments.size() > 0) {
            builder.startObject(Fields.ATTACHMENTS.getPreferredName());
            for (EmailAttachmentParser.EmailAttachment attachment : attachments) {
                attachment.toXContent(builder, params);
            }
            builder.endObject();
        }

        return builder;
    }
}
