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

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.support.ldap.LdapSslSocketFactory;
import org.elasticsearch.shield.support.AbstractShieldModule;

import static org.elasticsearch.common.inject.name.Names.named;

/**
 * Configures ActiveDirectory Realm object injections
 */
public class ActiveDirectoryModule extends AbstractShieldModule.Node {

    private final boolean enabled;

    public ActiveDirectoryModule(Settings settings) {
        super(settings);
        enabled = enabled(settings);
    }

    @Override
    protected void configureNode() {
        if (enabled) {
            /* This socket factory needs to be configured before any LDAP connections are created.  LDAP configuration
            for JNDI invokes a static getSocketFactory method from LdapSslSocketFactory.  */
            requestStaticInjection(LdapSslSocketFactory.class);

            bind(Realm.class).annotatedWith(named(ActiveDirectoryRealm.type)).to(ActiveDirectoryRealm.class).asEagerSingleton();
        } else {
            bind(ActiveDirectoryRealm.class).toProvider(Providers.<ActiveDirectoryRealm>of(null));
        }
    }

    static boolean enabled(Settings settings) {
        Settings authcSettings = settings.getAsSettings("shield.authc");
        if (!authcSettings.names().contains(ActiveDirectoryRealm.type)) {
            return false;
        }
        Settings ldapSettings = authcSettings.getAsSettings(ActiveDirectoryRealm.type);
        return ldapSettings.getAsBoolean("enabled", true);
    }
}
