package org.elasticsearch.shield.authc.ldap;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public class LdapModule extends AbstractModule {

    public static boolean enabled(Settings settings) {
        Settings ldapSettings = settings.getComponentSettings(LdapModule.class);
        return ldapSettings != null;
    }

    @Override
    protected void configure() {

    }
}
