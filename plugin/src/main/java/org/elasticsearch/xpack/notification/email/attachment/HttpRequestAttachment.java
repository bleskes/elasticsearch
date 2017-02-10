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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.common.http.HttpRequestTemplate;

import java.io.IOException;
import java.util.Objects;

public class HttpRequestAttachment implements EmailAttachmentParser.EmailAttachment {

    private final HttpRequestTemplate requestTemplate;
    private boolean inline;
    private final String contentType;
    private final String id;

    public HttpRequestAttachment(String id, HttpRequestTemplate requestTemplate, boolean inline, @Nullable String contentType) {
        this.id = id;
        this.requestTemplate = requestTemplate;
        this.inline = inline;
        this.contentType = contentType;
    }

    public HttpRequestTemplate getRequestTemplate() {
        return requestTemplate;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean inline() {
        return inline;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(id)
                .startObject(HttpEmailAttachementParser.TYPE)
                .field(HttpEmailAttachementParser.Fields.REQUEST.getPreferredName(), requestTemplate, params);
        if (Strings.hasLength(contentType)) {
            builder.field(HttpEmailAttachementParser.Fields.CONTENT_TYPE.getPreferredName(), contentType);
        }
        if (inline) {
            builder.field(HttpEmailAttachementParser.Fields.INLINE.getPreferredName(), inline);
        }
        return builder.endObject().endObject();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public String type() {
        return HttpEmailAttachementParser.TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpRequestAttachment otherDataAttachment = (HttpRequestAttachment) o;
        return Objects.equals(id, otherDataAttachment.id) && Objects.equals(requestTemplate, otherDataAttachment.requestTemplate)
                && Objects.equals(contentType, otherDataAttachment.contentType) && Objects.equals(inline, otherDataAttachment.inline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestTemplate, contentType, inline);
    }

    public static class Builder {

        private String id;
        private HttpRequestTemplate httpRequestTemplate;
        private String contentType;
        private boolean inline = false;

        private Builder(String id) {
            this.id = id;
        }

        public Builder httpRequestTemplate(HttpRequestTemplate httpRequestTemplate) {
            this.httpRequestTemplate = httpRequestTemplate;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder inline(boolean inline) {
            this.inline = inline;
            return this;
        }

        public HttpRequestAttachment build() {
            return new HttpRequestAttachment(id, httpRequestTemplate, inline, contentType);
        }

    }
}
