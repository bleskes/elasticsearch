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

package org.elasticsearch.shield.authc.ldap;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapRealm;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 * Authenticates username/password tokens against ldap, locates groups and maps them to roles.
 */
public class LdapRealm extends AbstractLdapRealm {

    public static final String TYPE = "ldap";

    public LdapRealm(RealmConfig config, LdapConnectionFactory ldap, LdapGroupToRoleMapper roleMapper) {
        super(TYPE, config, ldap, roleMapper);
    }

    public static class Factory extends AbstractLdapRealm.Factory<LdapRealm> {

        private final ResourceWatcherService watcherService;

        @Inject
        public Factory(ResourceWatcherService watcherService, RestController restController) {
            super(TYPE, restController);
            this.watcherService = watcherService;
        }

        @Override
        public LdapRealm create(RealmConfig config) {
            LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);
            LdapGroupToRoleMapper roleMapper = new LdapGroupToRoleMapper(config, watcherService);
            return new LdapRealm(config, connectionFactory, roleMapper);
        }
    }
}
