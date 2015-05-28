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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.activedirectory.ActiveDirectoryRealm;
import org.elasticsearch.shield.authc.esusers.ESUsersRealm;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.authc.pki.PkiRealm;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class AuthenticationModule extends AbstractShieldModule.Node {

    public AuthenticationModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        MapBinder<String, Realm.Factory> mapBinder = MapBinder.newMapBinder(binder(), String.class, Realm.Factory.class);
        mapBinder.addBinding(ESUsersRealm.TYPE).to(ESUsersRealm.Factory.class).asEagerSingleton();
        mapBinder.addBinding(ActiveDirectoryRealm.TYPE).to(ActiveDirectoryRealm.Factory.class).asEagerSingleton();
        mapBinder.addBinding(LdapRealm.TYPE).to(LdapRealm.Factory.class).asEagerSingleton();
        mapBinder.addBinding(PkiRealm.TYPE).to(PkiRealm.Factory.class).asEagerSingleton();

        bind(Realms.class).asEagerSingleton();
        bind(AnonymousService.class).asEagerSingleton();
        bind(AuthenticationService.class).to(InternalAuthenticationService.class).asEagerSingleton();
    }
}
