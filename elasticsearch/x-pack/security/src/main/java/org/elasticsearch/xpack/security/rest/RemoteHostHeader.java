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

package org.elasticsearch.xpack.security.rest;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestRequest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *
 */
public class RemoteHostHeader {

    static final String KEY = "_rest_remote_address";

    /**
     * Extracts the remote address from the given rest request and puts in the request context. This will
     * then be copied to the subsequent action requests.
     */
    public static void process(RestRequest request, ThreadContext threadContext) {
        threadContext.putTransient(KEY, request.getRemoteAddress());
    }

    /**
     * Extracts the rest remote address from the message context. If not found, returns {@code null}. transport
     * messages that were created by rest handlers, should have this in their context.
     */
    public static InetSocketAddress restRemoteAddress(ThreadContext threadContext) {
        SocketAddress address = threadContext.getTransient(KEY);
        if (address != null && address instanceof InetSocketAddress) {
            return (InetSocketAddress) address;
        }
        return null;
    }

    public static void putRestRemoteAddress(ThreadContext threadContext, SocketAddress address) {
        threadContext.putTransient(KEY, address);
    }
}
