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

import java.io.IOException;
import java.util.Locale;

public enum Scheme implements ToXContent {

    HTTP("http", 80),
    HTTPS("https", 443);

    private final String scheme;
    private final int defaultPort;

    Scheme(String scheme, int defaultPort) {
        this.scheme = scheme;
        this.defaultPort = defaultPort;
    }

    public String scheme() {
        return scheme;
    }

    public int defaultPort() {
        return defaultPort;
    }

    public static Scheme parse(String value) {
        value = value.toLowerCase(Locale.ROOT);
        switch (value) {
            case "http":
                return HTTP;
            case "https":
                return HTTPS;
            default:
                throw new IllegalArgumentException("unsupported http scheme [" + value + "]");
        }
    }


    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(name().toLowerCase(Locale.ROOT));
    }
}
