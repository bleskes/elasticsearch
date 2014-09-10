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

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.esusers.ESUsersModule;
import org.elasticsearch.shield.authc.ldap.LdapModule;
import org.elasticsearch.shield.authc.system.SystemRealm;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class AuthenticationModule extends AbstractShieldModule.Node.Spawn {

    public AuthenticationModule(Settings settings) {
        super(settings);
    }

    @Override
    public Iterable<? extends Node> spawnModules() {
        return ImmutableList.of(
                new SystemRealm.Module(settings),
                new ESUsersModule(settings),
                new LdapModule(settings));
    }

    @Override
    protected void configureNode() {
        bind(AuthenticationService.class).to(InternalAuthenticationService.class).asEagerSingleton();
    }
}
