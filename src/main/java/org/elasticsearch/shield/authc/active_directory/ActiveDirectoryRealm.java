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
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapRealm;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 *
 */
public class ActiveDirectoryRealm extends AbstractLdapRealm {

    public static final String TYPE = "active_directory";

    @Inject
    public ActiveDirectoryRealm(RealmConfig config,
                                ActiveDirectoryConnectionFactory connectionFactory,
                                ActiveDirectoryGroupToRoleMapper roleMapper) {

        super(TYPE, config, connectionFactory, roleMapper);
    }

    public static class Factory extends AbstractLdapRealm.Factory<ActiveDirectoryRealm> {

        private final ResourceWatcherService watcherService;

        @Inject
        public Factory(ResourceWatcherService watcherService, RestController restController) {
            super(ActiveDirectoryRealm.TYPE, restController);
            this.watcherService = watcherService;
        }

        @Override
        public ActiveDirectoryRealm create(RealmConfig config) {
            ActiveDirectoryConnectionFactory connectionFactory = new ActiveDirectoryConnectionFactory(config);
            ActiveDirectoryGroupToRoleMapper roleMapper = new ActiveDirectoryGroupToRoleMapper(config, watcherService);
            return new ActiveDirectoryRealm(config, connectionFactory, roleMapper);
        }
    }
}
