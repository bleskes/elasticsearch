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

package org.elasticsearch.shield.authc.esnative;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;

import java.util.List;

/**
 * User/password realm that is backed by an Elasticsearch index
 */
public class NativeRealm extends CachingUsernamePasswordRealm {

    public static final String TYPE = "native";

    final NativeUsersStore userStore;

    public NativeRealm(RealmConfig config, NativeUsersStore usersStore) {
        super(TYPE, config);
        this.userStore = usersStore;
        usersStore.addListener(new Listener());
    }

    @Override
    public boolean userLookupSupported() {
        return true;
    }

    @Override
    protected User doLookupUser(String username) {
        return userStore.getUser(username);
    }
    
    @Override
    protected User doAuthenticate(UsernamePasswordToken token) {
        return userStore.verifyPassword(token.principal(), token.credentials());
    }

    class Listener implements NativeUsersStore.ChangeListener {

        @Override
        public void onUsersChanged(List<String> usernames) {
            for (String username : usernames) {
                expire(username);
            }
        }
    }

    public static class Factory extends Realm.Factory<NativeRealm> {

        private final Settings settings;
        private final Environment env;
        private final NativeUsersStore userStore;

        @Inject
        public Factory(Settings settings, Environment env, NativeUsersStore userStore) {
            super(TYPE, true);
            this.settings = settings;
            this.env = env;
            this.userStore = userStore;
        }

        @Override
        public NativeRealm create(RealmConfig config) {
            return new NativeRealm(config, userStore);
        }

        @Override
        public NativeRealm createDefault(String name) {
            RealmConfig config = new RealmConfig(name, Settings.EMPTY, settings, env);
            return create(config);
        }
    }
}
