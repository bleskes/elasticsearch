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

package org.elasticsearch.xpack.security.transport;

import org.jboss.netty.handler.ssl.NotSslRecordException;

import javax.net.ssl.SSLException;

public class SSLExceptionHelper {

    private SSLExceptionHelper() {
    }

    public static boolean isNotSslRecordException(Throwable e) {
        return e instanceof NotSslRecordException && e.getCause() == null;
    }

    public static boolean isCloseDuringHandshakeException(Throwable e) {
        return e instanceof SSLException
                && e.getCause() == null
                && "Received close_notify during handshake".equals(e.getMessage());
    }
}
