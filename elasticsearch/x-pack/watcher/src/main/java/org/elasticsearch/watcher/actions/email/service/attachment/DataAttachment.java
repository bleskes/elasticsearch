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

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class DataAttachment implements EmailAttachmentParser.EmailAttachment {

    private final String id;
    private final org.elasticsearch.watcher.actions.email.DataAttachment dataAttachment;

    public DataAttachment(String id, org.elasticsearch.watcher.actions.email.DataAttachment dataAttachment) {
        this.id = id;
        this.dataAttachment = dataAttachment;
    }

    public org.elasticsearch.watcher.actions.email.DataAttachment getDataAttachment() {
        return dataAttachment;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(id).startObject(DataAttachmentParser.TYPE);
        if (dataAttachment == org.elasticsearch.watcher.actions.email.DataAttachment.YAML) {
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

    public static Builder builder(String id) {
        return new Builder(id);
    }


    public static class Builder {

        private String id;
        private org.elasticsearch.watcher.actions.email.DataAttachment dataAttachment;

        private Builder(String id) {
            this.id = id;
        }

        public Builder dataAttachment(org.elasticsearch.watcher.actions.email.DataAttachment dataAttachment) {
            this.dataAttachment = dataAttachment;
            return this;
        }

        public DataAttachment build() {
            return new DataAttachment(id, dataAttachment);
        }
    }
}
