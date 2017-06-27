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

package org.elasticsearch.xpack.deprecation;


import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Information about deprecated items
 */
public class DeprecationIssue implements Writeable, ToXContent {

    public enum Level implements Writeable {
        NONE,
        INFO,
        WARNING,
        CRITICAL
        ;

        public static Level fromString(String value) {
            return Level.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Level readFromStream(StreamInput in) throws IOException {
            int ordinal = in.readVInt();
            if (ordinal < 0 || ordinal >= values().length) {
                throw new IOException("Unknown Level ordinal [" + ordinal + "]");
            }
            return values()[ordinal];
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(ordinal());
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private Level level;
    private String message;
    private String url;
    private String details;

    // pkg-private for tests
    DeprecationIssue() {

    }

    public DeprecationIssue(Level level, String message, String url, @Nullable String details) {
        this.level = level;
        this.message = message;
        this.url = url;
        this.details = details;
    }

    public DeprecationIssue(StreamInput in) throws IOException {
        level = Level.readFromStream(in);
        message = in.readString();
        url = in.readString();
        details = in.readOptionalString();
    }


    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        level.writeTo(out);
        out.writeString(message);
        out.writeString(url);
        out.writeOptionalString(details);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject()
            .field("level", level)
            .field("message", message)
            .field("url", url);
        if (details != null) {
            builder.field("details", details);
        }
        return builder.endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeprecationIssue that = (DeprecationIssue) o;
        return Objects.equals(level, that.level) &&
            Objects.equals(message, that.message) &&
            Objects.equals(url, that.url) &&
            Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, message, url, details);
    }
}

