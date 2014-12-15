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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapRealm;
import org.elasticsearch.watcher.ResourceWatcherService;

/**
 * Authenticates username/password tokens against ldap, locates groups and maps them to roles.
 */
public class LdapRealm extends AbstractLdapRealm {

    public static final String TYPE = "ldap";

    @Inject
    public LdapRealm(String name, Settings settings, LdapConnectionFactory ldap, LdapGroupToRoleMapper roleMapper) {
        super(name, TYPE, settings, ldap, roleMapper);
    }

    @Override
    public String type() {
        return TYPE;
    }

    public static class Factory extends AbstractLdapRealm.Factory<LdapRealm> {

        private final Environment env;
        private final ResourceWatcherService watcherService;

        @Inject
        public Factory(Environment env, ResourceWatcherService watcherService, RestController restController) {
            super(TYPE, restController);
            this.env = env;
            this.watcherService = watcherService;
        }

        @Override
        public LdapRealm create(String name, Settings settings) {
            LdapConnectionFactory connectionFactory = new LdapConnectionFactory(settings);
            LdapGroupToRoleMapper roleMapper = new LdapGroupToRoleMapper(settings, name, env, watcherService);
            return new LdapRealm(name, settings, connectionFactory, roleMapper);
        }
    }
}
