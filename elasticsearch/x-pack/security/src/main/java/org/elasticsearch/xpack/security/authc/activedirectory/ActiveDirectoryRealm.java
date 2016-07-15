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

package org.elasticsearch.xpack.security.authc.activedirectory;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.ldap.support.AbstractLdapRealm;
import org.elasticsearch.xpack.security.authc.support.DnRoleMapper;
import org.elasticsearch.xpack.security.ssl.ClientSSLService;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 *
 */
public class ActiveDirectoryRealm extends AbstractLdapRealm {

    public static final String TYPE = "active_directory";

    public ActiveDirectoryRealm(RealmConfig config,
                                ActiveDirectorySessionFactory connectionFactory,
                                DnRoleMapper roleMapper) {

        super(TYPE, config, connectionFactory, roleMapper);
    }

    public static class Factory extends AbstractLdapRealm.Factory<ActiveDirectoryRealm> {

        private final ResourceWatcherService watcherService;
        private final ClientSSLService clientSSLService;

        @Inject
        public Factory(ResourceWatcherService watcherService, ClientSSLService clientSSLService) {
            super(ActiveDirectoryRealm.TYPE);
            this.watcherService = watcherService;
            this.clientSSLService = clientSSLService;
        }

        @Override
        public ActiveDirectoryRealm create(RealmConfig config) {
            ActiveDirectorySessionFactory connectionFactory = new ActiveDirectorySessionFactory(config, clientSSLService).init();
            DnRoleMapper roleMapper = new DnRoleMapper(TYPE, config, watcherService, null);
            return new ActiveDirectoryRealm(config, connectionFactory, roleMapper);
        }
    }
}
