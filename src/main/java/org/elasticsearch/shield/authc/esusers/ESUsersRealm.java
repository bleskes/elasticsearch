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

package org.elasticsearch.shield.authc.esusers;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.shield.authc.support.RefreshListener;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 *
 */
public class ESUsersRealm extends CachingUsernamePasswordRealm {

    public static final String TYPE = "esusers";

    final FileUserPasswdStore userPasswdStore;
    final FileUserRolesStore userRolesStore;

    public ESUsersRealm(String name, Settings settings, FileUserPasswdStore userPasswdStore, FileUserRolesStore userRolesStore) {
        super(name, TYPE, settings);
        Listener listener = new Listener();
        this.userPasswdStore = userPasswdStore;
        userPasswdStore.addListener(listener);
        this.userRolesStore = userRolesStore;
        userRolesStore.addListener(listener);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public UsernamePasswordToken token(RestRequest request) {
        return UsernamePasswordToken.extractToken(request, null);
    }

    @Override
    public UsernamePasswordToken token(TransportMessage<?> message) {
        return UsernamePasswordToken.extractToken(message, null);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    @Override
    protected User doAuthenticate(UsernamePasswordToken token) {
        if (!userPasswdStore.verifyPassword(token.principal(), token.credentials())) {
            return null;
        }
        String[] roles = userRolesStore.roles(token.principal());
        return new User.Simple(token.principal(), roles);
    }

    class Listener implements RefreshListener {
        @Override
        public void onRefresh() {
            expireAll();
        }
    }

    public static class Factory extends Realm.Factory<ESUsersRealm> {

        private final Environment env;
        private final ResourceWatcherService watcherService;

        @Inject
        public Factory(Environment env, ResourceWatcherService watcherService, RestController restController) {
            super(TYPE, true);
            this.env = env;
            this.watcherService = watcherService;
            restController.registerRelevantHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
        }

        @Override
        public ESUsersRealm create(String name, Settings settings) {
            FileUserPasswdStore userPasswdStore = new FileUserPasswdStore(settings, env, watcherService);
            FileUserRolesStore userRolesStore = new FileUserRolesStore(settings, env, watcherService);
            return new ESUsersRealm(name, settings, userPasswdStore, userRolesStore);
        }

        @Override
        public ESUsersRealm createDefault(String name) {
            return create(name, ImmutableSettings.EMPTY);
        }
    }
}
