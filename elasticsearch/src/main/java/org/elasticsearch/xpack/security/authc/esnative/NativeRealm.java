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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.user.User;

import java.util.HashSet;
import java.util.Set;

/**
 * User/password realm that is backed by an Elasticsearch index
 */
public class NativeRealm extends CachingUsernamePasswordRealm {

    public static final String TYPE = "native";

    private final NativeUsersStore userStore;

    public NativeRealm(RealmConfig config, NativeUsersStore usersStore) {
        super(TYPE, config);
        this.userStore = usersStore;
    }

    @Override
    public boolean userLookupSupported() {
        return true;
    }

    @Override
    protected void doLookupUser(String username, ActionListener<User> listener) {
        userStore.getUser(username, listener);
    }

    @Override
    protected void doAuthenticate(UsernamePasswordToken token, ActionListener<User> listener) {
        userStore.verifyPassword(token.principal(), token.credentials(), listener);
    }

    /**
     * @return The {@link Setting setting configuration} for this realm type
     */
    public static Set<Setting<?>> getSettings() {
        return CachingUsernamePasswordRealm.getCachingSettings();
    }
}
