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

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class DataAttachment implements EmailAttachmentParser.EmailAttachment {

    private final String id;
    private final org.elasticsearch.xpack.notification.email.DataAttachment dataAttachment;

    public DataAttachment(String id, org.elasticsearch.xpack.notification.email.DataAttachment dataAttachment) {
        this.id = id;
        this.dataAttachment = dataAttachment;
    }

    public org.elasticsearch.xpack.notification.email.DataAttachment getDataAttachment() {
        return dataAttachment;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(id).startObject(DataAttachmentParser.TYPE);
        if (dataAttachment == org.elasticsearch.xpack.notification.email.DataAttachment.YAML) {
            builder.field("format", "yaml");
        } else {
            builder.field("format", "json");
        }
        return builder.endObject().endObject();
    }

    @Override
    public String type() {
        return DataAttachmentParser.TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataAttachment otherDataAttachment = (DataAttachment) o;
        return Objects.equals(id, otherDataAttachment.id) && Objects.equals(dataAttachment, otherDataAttachment.dataAttachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dataAttachment);
    }

    public String id() {
        return id;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }


    public static class Builder {

        private String id;
        private org.elasticsearch.xpack.notification.email.DataAttachment dataAttachment;

        private Builder(String id) {
            this.id = id;
        }

        public Builder dataAttachment(org.elasticsearch.xpack.notification.email.DataAttachment dataAttachment) {
            this.dataAttachment = dataAttachment;
            return this;
        }

        public DataAttachment build() {
            return new DataAttachment(id, dataAttachment);
        }
    }
}
