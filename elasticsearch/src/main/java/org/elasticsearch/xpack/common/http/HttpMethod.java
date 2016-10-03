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

/**
 */
public enum HttpMethod implements ToXContent {

    HEAD("HEAD"),
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    private final String method;

    HttpMethod(String method) {
        this.method = method;
    }

    public String method() {
        return method;
    }

    public static HttpMethod parse(String value) {
        value = value.toUpperCase(Locale.ROOT);
        switch (value) {
            case "HEAD":
                return HEAD;
            case "GET":
                return GET;
            case "POST":
                return POST;
            case "PUT":
                return PUT;
            case "DELETE":
                return DELETE;
            default:
                throw new IllegalArgumentException("unsupported http method [" + value + "]");
        }
    }


    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(name().toLowerCase(Locale.ROOT));
    }
}
