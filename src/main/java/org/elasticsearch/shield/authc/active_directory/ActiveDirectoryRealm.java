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

package org.elasticsearch.shield.authc.active_directory;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapRealm;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 *
 */
public class ActiveDirectoryRealm extends AbstractLdapRealm {

    public static final String TYPE = "active_directory";

    @Inject
    public ActiveDirectoryRealm(String name, Settings settings, ActiveDirectoryConnectionFactory connectionFactory,
                                ActiveDirectoryGroupToRoleMapper roleMapper) {

        super(name, TYPE, settings, connectionFactory, roleMapper);
    }

    @Override
    public String type() {
        return TYPE;
    }


    public static class Factory extends AbstractLdapRealm.Factory<ActiveDirectoryRealm> {

        private final Environment env;
        private final ResourceWatcherService watcherService;

        @Inject
        public Factory(Environment env, ResourceWatcherService watcherService, RestController restController) {
            super(ActiveDirectoryRealm.TYPE, restController);
            this.env = env;
            this.watcherService = watcherService;
        }

        @Override
        public ActiveDirectoryRealm create(String name, Settings settings) {
            ActiveDirectoryConnectionFactory connectionFactory = new ActiveDirectoryConnectionFactory(settings);
            ActiveDirectoryGroupToRoleMapper roleMapper = new ActiveDirectoryGroupToRoleMapper(settings, env, watcherService);
            return new ActiveDirectoryRealm(name, settings, connectionFactory, roleMapper);
        }
    }
}
