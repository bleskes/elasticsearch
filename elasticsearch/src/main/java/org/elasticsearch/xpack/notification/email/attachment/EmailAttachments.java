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

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class EmailAttachments implements ToXContent {

    public static final EmailAttachments EMPTY_ATTACHMENTS = new EmailAttachments(
            Collections.<EmailAttachmentParser.EmailAttachment>emptyList());

    public interface Fields {
        ParseField ATTACHMENTS = new ParseField("attachments");
    }

    private final Collection<EmailAttachmentParser.EmailAttachment> attachments;

    public EmailAttachments(Collection<EmailAttachmentParser.EmailAttachment> attachments) {
        this.attachments = attachments;
    }

    public Collection<EmailAttachmentParser.EmailAttachment> getAttachments() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EmailAttachments other = (EmailAttachments) o;
        return Objects.equals(attachments, other.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachments);
    }
}
