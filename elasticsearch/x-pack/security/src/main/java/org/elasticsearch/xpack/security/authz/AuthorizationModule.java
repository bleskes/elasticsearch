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

package org.elasticsearch.xpack.security.authz;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.authz.store.CompositeRolesStore;
import org.elasticsearch.xpack.security.authz.store.FileRolesStore;
import org.elasticsearch.xpack.security.authz.store.NativeRolesStore;
import org.elasticsearch.xpack.security.authz.store.ReservedRolesStore;
import org.elasticsearch.xpack.security.authz.store.RolesStore;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;

/**
 * Module used to bind various classes necessary for authorization
 */
public class AuthorizationModule extends AbstractSecurityModule.Node {

    public AuthorizationModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {

        // First the file and native roles stores must be bound...
        bind(ReservedRolesStore.class).asEagerSingleton();
        bind(FileRolesStore.class).asEagerSingleton();
        bind(NativeRolesStore.class).asEagerSingleton();
        // Then the composite roles store (which combines both) can be bound
        bind(RolesStore.class).to(CompositeRolesStore.class).asEagerSingleton();
        bind(AuthorizationService.class).to(InternalAuthorizationService.class).asEagerSingleton();
    }

}
