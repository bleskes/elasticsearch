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

package org.elasticsearch.xpack.security;

import java.io.IOException;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.User;

/**
 * A lightweight utility that can find the current user and authentication information for the local thread.
 */
public class SecurityContext {

    private final ESLogger logger;
    private final ThreadContext threadContext;
    private final CryptoService cryptoService;
    private final boolean signUserHeader;

    /**
     * Creates a new security context.
     * If cryptoService is null, security is disabled and {@link #getUser()}
     * and {@link #getAuthentication()} will always return null.
     */
    public SecurityContext(Settings settings, ThreadPool threadPool, CryptoService cryptoService) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.threadContext = threadPool.getThreadContext();
        this.cryptoService = cryptoService;
        this.signUserHeader = AuthenticationService.SIGN_USER_HEADER.get(settings);
    }

    /** Returns the current user information, or null if the current request has no authentication info. */
    public User getUser() {
        Authentication authentication = getAuthentication();
        return authentication == null ? null : authentication.getUser();
    }

    /** Returns the authentication information, or null if the current request has no authentication info. */
    public Authentication getAuthentication() {
        if (cryptoService == null) {
            return null;
        }
        try {
            return Authentication.readFromContext(threadContext, cryptoService, signUserHeader);
        } catch (IOException e) {
            // TODO: this seems bogus, the only way to get an ioexception here is from a corrupt or tampered
            // auth header, which should be be audited?
            logger.error("failed to read authentication", e);
            return null;
        }
    }
}
