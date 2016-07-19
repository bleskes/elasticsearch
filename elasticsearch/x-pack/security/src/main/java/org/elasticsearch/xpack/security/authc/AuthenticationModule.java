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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;
import org.elasticsearch.xpack.security.user.AnonymousUser;

/**
 *
 */
public class AuthenticationModule extends AbstractSecurityModule.Node {

    private Class<? extends AuthenticationFailureHandler> authcFailureHandler = null;

    public AuthenticationModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        if (!securityEnabled) {
            bind(Realms.class).toProvider(Providers.of(null));
            return;
        }

        AnonymousUser.initialize(settings);
        if (authcFailureHandler == null) {
            bind(AuthenticationFailureHandler.class).to(DefaultAuthenticationFailureHandler.class).asEagerSingleton();
        } else {
            bind(AuthenticationFailureHandler.class).to(authcFailureHandler).asEagerSingleton();
        }
        bind(AuthenticationService.class).asEagerSingleton();
    }

    /**
     * Sets the {@link AuthenticationFailureHandler} to the specified implementation
     */
    public void setAuthenticationFailureHandler(Class<? extends AuthenticationFailureHandler> clazz) {
        this.authcFailureHandler = clazz;
    }
}
