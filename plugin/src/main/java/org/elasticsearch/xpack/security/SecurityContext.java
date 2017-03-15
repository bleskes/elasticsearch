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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.util.concurrent.ThreadContext.StoredContext;
import org.elasticsearch.node.Node;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A lightweight utility that can find the current user and authentication information for the local thread.
 */
public class SecurityContext {

    private final Logger logger;
    private final ThreadContext threadContext;
    private final CryptoService cryptoService;
    private final Settings settings;
    private final String nodeName;
    private final boolean signingEnabled;

    /**
     * Creates a new security context.
     * If cryptoService is null, security is disabled and {@link #getUser()}
     * and {@link #getAuthentication()} will always return null.
     */
    public SecurityContext(Settings settings, ThreadContext threadContext, CryptoService cryptoService) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.threadContext = threadContext;
        this.cryptoService = cryptoService;
        this.nodeName = Node.NODE_NAME_SETTING.get(settings);
        this.settings = settings;
        this.signingEnabled = AuthenticationService.SIGN_USER_HEADER.get(settings);
    }

    /** Returns the current user information, or null if the current request has no authentication info. */
    public User getUser() {
        Authentication authentication = getAuthentication();
        return authentication == null ? null : authentication.getUser();
    }

    /** Returns the authentication information, or null if the current request has no authentication info. */
    public Authentication getAuthentication() {
        try {
            return Authentication.readFromContext(threadContext, cryptoService, settings, Version.CURRENT, signingEnabled);
        } catch (IOException e) {
            // TODO: this seems bogus, the only way to get an ioexception here is from a corrupt or tampered
            // auth header, which should be be audited?
            logger.error("failed to read authentication", e);
            return null;
        }
    }

    /**
     * Sets the user forcefully to the provided user and serializes the user in a format that is compatible with the provided version.
     * There must not be an existing user in the ThreadContext otherwise an exception will be thrown. This method is package private for
     * testing.
     */
    void setUser(User user, Version version) {
        Objects.requireNonNull(user);
        final Authentication.RealmRef lookedUpBy;
        if (user.runAs() == null) {
            lookedUpBy = null;
        } else {
            lookedUpBy = new Authentication.RealmRef("__attach", "__attach", nodeName);
        }

        try {
            Authentication authentication =
                    new Authentication(user, new Authentication.RealmRef("__attach", "__attach", nodeName), lookedUpBy, version);
            authentication.writeToContext(threadContext, cryptoService, settings, version, signingEnabled);
        } catch (IOException e) {
            throw new AssertionError("how can we have a IOException with a user we set", e);
        }
    }

    /**
     * Runs the consumer in a new context as the provided user and serializes the user in the ThreadContext in a format that is
     * compatible with the provided version. The original context is provided to the consumer. When this method returns, the original
     * context is restored.
     */
    public void executeAsUser(User user, Consumer<StoredContext> consumer, Version version) {
        final StoredContext original = threadContext.newStoredContext(true);
        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            setUser(user, version);
            consumer.accept(original);
        }
    }
}
