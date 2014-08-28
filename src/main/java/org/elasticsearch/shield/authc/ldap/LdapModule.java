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

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.support.AbstractShieldModule;

import static org.elasticsearch.common.inject.name.Names.named;

/**
 * Configures Ldap object injections
 */
public class LdapModule extends AbstractShieldModule.Node {

    private final boolean enabled;

    public LdapModule(Settings settings) {
        super(settings);
        enabled = enabled(settings);
    }

    @Override
    protected void configureNode() {
        if (enabled) {
            bind(Realm.class).annotatedWith(named(LdapRealm.TYPE)).to(LdapRealm.class).asEagerSingleton();
            bind(LdapSslSocketFactory.class).asEagerSingleton();
            bind(LdapGroupToRoleMapper.class).asEagerSingleton();
            String mode = settings.getComponentSettings(LdapModule.class).get("mode", "ldap");
            if ("ldap".equals(mode)) {
                bind(LdapConnectionFactory.class).to(StandardLdapConnectionFactory.class);
            } else {
                bind(LdapConnectionFactory.class).to(ActiveDirectoryConnectionFactory.class);
            }
        } else {
            bind(LdapRealm.class).toProvider(Providers.of((LdapRealm) null));
        }
    }

    static boolean enabled(Settings settings) {
        Settings authcSettings = settings.getAsSettings("shield.authc");
        if (!authcSettings.names().contains("ldap")) {
            return false;
        }
        Settings ldapSettings = authcSettings.getAsSettings("ldap");
        return ldapSettings.getAsBoolean("enabled", true);
    }
}
