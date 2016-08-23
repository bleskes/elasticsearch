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

package org.elasticsearch.xpack.security.authc.esnative;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore.ReservedUserInfo;
import org.elasticsearch.xpack.security.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.xpack.security.authc.support.Hasher;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.support.Exceptions;
import org.elasticsearch.xpack.security.user.AnonymousUser;
import org.elasticsearch.xpack.security.user.ElasticUser;
import org.elasticsearch.xpack.security.user.KibanaUser;
import org.elasticsearch.xpack.security.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A realm for predefined users. These users can only be modified in terms of changing their passwords; no other modifications are allowed.
 * This realm is <em>always</em> enabled.
 */
public class ReservedRealm extends CachingUsernamePasswordRealm {

    public static final String TYPE = "reserved";
    static final char[] DEFAULT_PASSWORD_HASH = Hasher.BCRYPT.hash(new SecuredString("changeme".toCharArray()));
    private static final ReservedUserInfo DEFAULT_USER_INFO = new ReservedUserInfo(DEFAULT_PASSWORD_HASH, true);

    private final NativeUsersStore nativeUsersStore;
    private final AnonymousUser anonymousUser;
    private final boolean anonymousEnabled;

    public ReservedRealm(Environment env, Settings settings, NativeUsersStore nativeUsersStore, AnonymousUser anonymousUser) {
        super(TYPE, new RealmConfig(TYPE, Settings.EMPTY, settings, env));
        this.nativeUsersStore = nativeUsersStore;
        this.anonymousUser = anonymousUser;
        this.anonymousEnabled = AnonymousUser.isAnonymousEnabled(settings);
    }

    @Override
    protected User doAuthenticate(UsernamePasswordToken token) {
        if (isReserved(token.principal(), config.globalSettings()) == false) {
            return null;
        }

        final ReservedUserInfo userInfo = getUserInfo(token.principal());
        if (userInfo != null) {
            try {
                if (Hasher.BCRYPT.verify(token.credentials(), userInfo.passwordHash)) {
                    return getUser(token.principal(), userInfo);
                }
            } finally {
                if (userInfo.passwordHash != DEFAULT_PASSWORD_HASH) {
                    Arrays.fill(userInfo.passwordHash, (char) 0);
                }
            }
        }
        // this was a reserved username - don't allow this to go to another realm...
        throw Exceptions.authenticationError("failed to authenticate user [{}]", token.principal());
    }

    @Override
    protected User doLookupUser(String username) {
        if (isReserved(username, config.globalSettings()) == false) {
            return null;
        }

        if (AnonymousUser.isAnonymousUsername(username, config.globalSettings())) {
            return anonymousEnabled ? anonymousUser : null;
        }

        final ReservedUserInfo userInfo = getUserInfo(username);
        if (userInfo != null) {
            return getUser(username, userInfo);
        }
        // this was a reserved username - don't allow this to go to another realm...
        throw Exceptions.authenticationError("failed to lookup user [{}]", username);
    }

    @Override
    public boolean userLookupSupported() {
        return true;
    }

    public static boolean isReserved(String username, Settings settings) {
        assert username != null;
        switch (username) {
            case ElasticUser.NAME:
            case KibanaUser.NAME:
                return true;
            default:
                return AnonymousUser.isAnonymousUsername(username, settings);
        }
    }

    User getUser(String username, ReservedUserInfo userInfo) {
        assert username != null;
        switch (username) {
            case ElasticUser.NAME:
                return new ElasticUser(userInfo.enabled);
            case KibanaUser.NAME:
                return new KibanaUser(userInfo.enabled);
            default:
                if (anonymousEnabled && anonymousUser.principal().equals(username)) {
                    return anonymousUser;
                }
                return null;
        }
    }

    public Collection<User> users() {
        if (nativeUsersStore.started() == false) {
            return anonymousEnabled ? Collections.singletonList(anonymousUser) : Collections.emptyList();
        }

        List<User> users = new ArrayList<>(3);
        try {
            Map<String, ReservedUserInfo> reservedUserInfos = nativeUsersStore.getAllReservedUserInfo();
            ReservedUserInfo userInfo = reservedUserInfos.get(ElasticUser.NAME);
            users.add(new ElasticUser(userInfo == null || userInfo.enabled));
            userInfo = reservedUserInfos.get(KibanaUser.NAME);
            users.add(new KibanaUser(userInfo == null || userInfo.enabled));
            if (anonymousEnabled) {
                users.add(anonymousUser);
            }
        } catch (Exception e) {
            logger.error("failed to retrieve reserved users", e);
            return anonymousEnabled ? Collections.singletonList(anonymousUser) : Collections.emptyList();
        }

        return users;
    }

    private ReservedUserInfo getUserInfo(final String username) {
        if (nativeUsersStore.started() == false) {
            // we need to be able to check for the user store being started...
            return null;
        }

        if (nativeUsersStore.securityIndexExists() == false) {
            return DEFAULT_USER_INFO;
        }

        try {
            ReservedUserInfo userInfo = nativeUsersStore.getReservedUserInfo(username);
            if (userInfo == null) {
                return DEFAULT_USER_INFO;
            }
            return userInfo;
        } catch (Exception e) {
            logger.error(
                    (Supplier<?>) () -> new ParameterizedMessage("failed to retrieve password hash for reserved user [{}]", username), e);
            return null;
        }
    }
}
