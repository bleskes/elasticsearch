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

package org.elasticsearch.shield.authc.esusers;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.support.UserPasswdStore;
import org.elasticsearch.shield.authc.support.UserRolesStore;

import static org.elasticsearch.common.inject.name.Names.named;

/**
 *
 */
public class ESUsersModule extends AbstractModule {

    public static boolean enabled(Settings settings) {
        return settings.getComponentSettings(ESUsersModule.class).getAsBoolean("enabled", true);
    }

    @Override
    protected void configure() {
        bind(Realm.class).annotatedWith(named(ESUsersRealm.TYPE)).to(ESUsersRealm.class).asEagerSingleton();
        bind(UserPasswdStore.class).annotatedWith(named("file")).to(FileUserPasswdStore.class).asEagerSingleton();
        bind(UserRolesStore.class).annotatedWith(named("file")).to(FileUserRolesStore.class).asEagerSingleton();
    }
}
