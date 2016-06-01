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

package org.elasticsearch.xpack.common.http;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Locale;

import static org.elasticsearch.xpack.support.Exceptions.illegalArgument;

/**
 */
public enum HttpContentType implements ToXContent {

    JSON() {
        @Override
        public XContentType contentType() {
            return XContentType.JSON;
        }
    },

    YAML() {
        @Override
        public XContentType contentType() {
            return XContentType.YAML;
        }
    },

    TEXT() {
        @Override
        public XContentType contentType() {
            return null;
        }
    };

    public abstract XContentType contentType();

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(id());
    }

    @Override
    public String toString() {
        return id();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HttpContentType resolve(String id) {
        switch (id.toLowerCase(Locale.ROOT)) {
            case "json" : return JSON;
            case "yaml":  return YAML;
            case "text":  return TEXT;
            default:
                throw illegalArgument("unknown http content type [{}]", id);
        }
    }
}
