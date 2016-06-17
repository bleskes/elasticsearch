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

package org.elasticsearch.xpack.security.authz.accesscontrol;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.transport.TransportRequest;

import java.util.Objects;

/**
 * A thread local based holder of the currnet {@link TransportRequest} instance.
 */
public final class RequestContext {

    // Need thread local to make the current transport request available to places in the code that
    // don't have direct access to the current transport request
    private static final ThreadLocal<RequestContext> current = new ThreadLocal<>();

    /**
     * If set then this returns the current {@link RequestContext} with the current {@link TransportRequest}.
     */
    public static RequestContext current() {
        return current.get();
    }

    /**
     * Invoked by the transport service to set the current transport request in the thread local
     */
    public static void setCurrent(RequestContext value) {
        current.set(value);
    }

    /**
     * Invoked by the transport service to remove the current request from the thread local
     */
    public static void removeCurrent() {
        current.remove();
    }

    private final ThreadContext threadContext;
    private final TransportRequest request;

    public RequestContext(TransportRequest request, ThreadContext threadContext) {
        this.request = Objects.requireNonNull(request);
        this.threadContext = Objects.requireNonNull(threadContext);
    }

    /**
     * @return current {@link TransportRequest}
     */
    public TransportRequest getRequest() {
        return request;
    }

    public ThreadContext getThreadContext() {
        return threadContext;
    }
}
