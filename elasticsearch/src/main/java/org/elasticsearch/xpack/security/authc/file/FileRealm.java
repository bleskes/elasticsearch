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

package org.elasticsearch.xpack.security.authc.file;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.user.User;

/**
 *
 */
public class FileRealm extends CachingUsernamePasswordRealm {

    public static final String TYPE = "file";

    final FileUserPasswdStore userPasswdStore;
    final FileUserRolesStore userRolesStore;

    public FileRealm(RealmConfig config, ResourceWatcherService watcherService) {
        this(config, new FileUserPasswdStore(config, watcherService), new FileUserRolesStore(config, watcherService));
    }

    // pkg private for testing
    FileRealm(RealmConfig config, FileUserPasswdStore userPasswdStore, FileUserRolesStore userRolesStore) {
        super(TYPE, config);
        this.userPasswdStore = userPasswdStore;
        userPasswdStore.addListener(this::expireAll);
        this.userRolesStore = userRolesStore;
        userRolesStore.addListener(this::expireAll);
    }

    @Override
    protected void doAuthenticate(UsernamePasswordToken token, ActionListener<User> listener) {
        if (userPasswdStore.verifyPassword(token.principal(), token.credentials())) {
            String[] roles = userRolesStore.roles(token.principal());
            listener.onResponse(new User(token.principal(), roles));
        } else {
            listener.onResponse(null);
        }
    }

    @Override
    protected void doLookupUser(String username, ActionListener<User> listener) {
        if (userPasswdStore.userExists(username)) {
            String[] roles = userRolesStore.roles(username);
            listener.onResponse(new User(username, roles));
        } else {
            listener.onResponse(null);
        }
    }

    @Override
    public Map<String, Object> usageStats() {
        Map<String, Object> stats = super.usageStats();
        // here we can determine the size based on the in mem user store
        stats.put("size", userPasswdStore.usersCount());
        return stats;
    }

    @Override
    public boolean userLookupSupported() {
        return true;
    }

    /**
     * @return The {@link Setting setting configuration} for this realm type
     */
    public static Set<Setting<?>> getSettings() {
        return CachingUsernamePasswordRealm.getCachingSettings();
    }
}
