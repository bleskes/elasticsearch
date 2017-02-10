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

package org.elasticsearch.test.http;

import org.elasticsearch.common.SuppressForbidden;

import java.util.Collections;
import java.util.List;

/**
 * A helper class to not leak the internal headers class into our tests
 * Currently setting multiple values for a single header is not supported, as it was not needed yet
 */
@SuppressForbidden(reason = "use http server")
public class Headers {

    final com.sun.net.httpserver.Headers headers;

    /**
     * Creates a class with empty headers
     */
    Headers() {
        this.headers = new com.sun.net.httpserver.Headers();
    }

    /**
     * Creates a class headers from http
     * @param headers The internal sun webserver headers object
     */
    Headers(com.sun.net.httpserver.Headers headers) {
        this.headers = headers;
    }

    /**
     * @param name The name of header
     * @return A list of values for this header
     */
    public List<String> get(String name) {
        return headers.get(name);
    }

    /**
     * Adds a new header to this headers object
     * @param name Name of the header
     * @param value Value of the header
     */
    void add(String name, String value) {
        this.headers.put(name, Collections.singletonList(value));
    }

    /**
     * @param name Name of the header
     * @return Returns the first header value or null if none exists
     */
    String getFirst(String name) {
        return headers.getFirst(name);
    }
}
