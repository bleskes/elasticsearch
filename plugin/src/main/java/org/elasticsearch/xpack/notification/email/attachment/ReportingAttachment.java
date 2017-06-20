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
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.common.http.HttpProxy;
import org.elasticsearch.xpack.common.http.auth.HttpAuth;

import java.io.IOException;
import java.util.Objects;

public class ReportingAttachment implements EmailAttachmentParser.EmailAttachment {

    static final ParseField INLINE = new ParseField("inline");
    static final ParseField AUTH = new ParseField("auth");
    static final ParseField PROXY = new ParseField("proxy");
    static final ParseField INTERVAL = new ParseField("interval");
    static final ParseField RETRIES = new ParseField("retries");
    static final ParseField URL = new ParseField("url");

    private final boolean inline;
    private final String id;
    private final HttpAuth auth;
    private final String url;
    private final TimeValue interval;
    private final Integer retries;
    private final HttpProxy proxy;

    ReportingAttachment(String id, String url, boolean inline, @Nullable TimeValue interval, @Nullable Integer retries,
                        @Nullable HttpAuth auth, @Nullable HttpProxy proxy) {
        this.id = id;
        this.url = url;
        this.retries = retries;
        this.inline = inline;
        this.auth = auth;
        this.interval = interval;
        this.proxy = proxy;
        if (retries != null && retries < 0) {
            throw new IllegalArgumentException("Retries for attachment must be >= 0");
        }
    }

    @Override
    public String type() {
        return ReportingAttachmentParser.TYPE;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean inline() {
        return inline;
    }

    public HttpAuth auth() {
        return auth;
    }

    public String url() {
        return url;
    }

    public TimeValue interval() {
        return interval;
    }

    public Integer retries() {
        return retries;
    }

    public HttpProxy proxy() {
        return proxy;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(id).startObject(ReportingAttachmentParser.TYPE)
                .field(URL.getPreferredName(), url);

        if (retries != null) {
            builder.field(RETRIES.getPreferredName(), retries);
        }

        if (interval != null) {
            builder.field(INTERVAL.getPreferredName(), interval);
        }

        if (inline) {
            builder.field(INLINE.getPreferredName(), inline);
        }

        if (auth != null) {
            builder.startObject(AUTH.getPreferredName());
            builder.field(auth.type(), auth, params);
            builder.endObject();
        }

        if (proxy != null) {
            proxy.toXContent(builder, params);
        }

        return builder.endObject().endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportingAttachment otherAttachment = (ReportingAttachment) o;
        return Objects.equals(id, otherAttachment.id) && Objects.equals(url, otherAttachment.url) &&
               Objects.equals(interval, otherAttachment.interval) && Objects.equals(inline, otherAttachment.inline) &&
               Objects.equals(retries, otherAttachment.retries) && Objects.equals(auth, otherAttachment.auth) &&
               Objects.equals(proxy, otherAttachment.proxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, interval, inline, retries, auth, proxy);
    }
}
