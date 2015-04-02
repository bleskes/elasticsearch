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

package org.elasticsearch.watcher.support.http;

import org.elasticsearch.ElasticsearchIllegalArgumentException;

import java.util.Locale;

public enum Scheme {

    HTTP("http"),
    HTTPS("https");

    private final String scheme;

    Scheme(String scheme) {
        this.scheme = scheme;
    }

    public String scheme() {
        return scheme;
    }

    public static Scheme parse(String value) {
        value = value.toLowerCase(Locale.ROOT);
        switch (value) {
            case "http":
                return HTTP;
            case "https":
                return HTTPS;
            default:
                throw new ElasticsearchIllegalArgumentException("unsupported http scheme [" + value + "]");
        }
    }
}
