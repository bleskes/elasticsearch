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

import java.net.URI;
import java.util.Locale;

/**
 * A request parsed by the MockWebServer
 */
public class MockRequest {

    private final String method;
    private final URI uri;
    private final Headers headers;
    private String body = null;

    @SuppressForbidden(reason = "use http server header class")
    MockRequest(String method, URI uri, com.sun.net.httpserver.Headers headers) {
        this.method = method;
        this.uri = uri;
        this.headers = new Headers(headers);
    }

    /**
     * @return The HTTP method of the incoming request
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return The URI of the incoming request
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @return The specific value of a request header, null if it does not exist
     */
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    /**
     * @return All headers associated with this request
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * @return The body the incoming request had, null if no body was found
     */
    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s", method, uri);
    }

    /**
     * @param body Sets the body of the incoming request
     */
    void setBody(String body) {
        this.body = body;
    }
}
