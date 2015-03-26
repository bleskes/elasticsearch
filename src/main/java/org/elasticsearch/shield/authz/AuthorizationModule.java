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

package org.elasticsearch.shield.authz;

import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.store.FileRolesStore;
import org.elasticsearch.shield.authz.store.RolesStore;
import org.elasticsearch.shield.support.AbstractShieldModule;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class AuthorizationModule extends AbstractShieldModule.Node {

    private final Set<Permission.Global.Role> reservedRoles = new HashSet<>();

    public AuthorizationModule(Settings settings) {
        super(settings);
    }

    public void registerReservedRole(Permission.Global.Role role) {
        reservedRoles.add(role);
    }

    @Override
    protected void configureNode() {

        Multibinder<Permission.Global.Role> reservedRolesBinder = Multibinder.newSetBinder(binder(), Permission.Global.Role.class);
        for (Permission.Global.Role reservedRole : reservedRoles) {
            reservedRolesBinder.addBinding().toInstance(reservedRole);
        }

        bind(FileRolesStore.class).asEagerSingleton();
        bind(RolesStore.class).to(FileRolesStore.class).asEagerSingleton();
        bind(AuthorizationService.class).to(InternalAuthorizationService.class).asEagerSingleton();
    }

}
