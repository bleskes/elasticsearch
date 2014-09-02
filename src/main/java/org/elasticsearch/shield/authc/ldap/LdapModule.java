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

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

/**
 * Configures Ldap object injections
 */
public class LdapModule extends AbstractModule {
    private final Settings settings;

    public LdapModule(Settings settings) {
        this.settings = settings;
    }

    public static boolean enabled(Settings settings) {
        Settings ldapSettings = settings.getComponentSettings(LdapModule.class);
        return ldapSettings != null && ldapSettings.getAsBoolean("enabled", true);
    }

    @Override
    protected void configure() {
        bind(LdapRealm.class).asEagerSingleton();
        bind(LdapGroupToRoleMapper.class).asEagerSingleton();
        String mode = settings.getComponentSettings(LdapModule.class).get("mode", "ldap");
        if ("ldap".equals(mode)) {
            bind(LdapConnectionFactory.class).to(StandardLdapConnectionFactory.class);
        } else {
            bind(LdapConnectionFactory.class).to(ActiveDirectoryConnectionFactory.class);
        }
    }
}
