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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.User;

public class AnonymousService {

    public static final String SETTING_AUTHORIZATION_EXCEPTION_ENABLED = "shield.authc.anonymous.authz_exception";
    static final String ANONYMOUS_USERNAME = "_es_anonymous_user";


    @Nullable
    private final User anonymousUser;
    private final boolean authzExceptionEnabled;

    @Inject
    public AnonymousService(Settings settings) {
        anonymousUser = resolveAnonymousUser(settings);
        authzExceptionEnabled = settings.getAsBoolean(SETTING_AUTHORIZATION_EXCEPTION_ENABLED, true);
    }

    public boolean enabled() {
        return anonymousUser != null;
    }

    public boolean isAnonymous(User user) {
        if (enabled()) {
            return anonymousUser.equals(user);
        }
        return false;
    }

    public User anonymousUser() {
        return anonymousUser;
    }

    public boolean authorizationExceptionsEnabled() {
        return authzExceptionEnabled;
    }

    static User resolveAnonymousUser(Settings settings) {
        String[] roles = settings.getAsArray("shield.authc.anonymous.roles", null);
        if (roles == null) {
            return null;
        }
        String username = settings.get("shield.authc.anonymous.username", ANONYMOUS_USERNAME);
        return new User.Simple(username, roles);
    }
}
