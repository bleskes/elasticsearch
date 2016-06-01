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

package org.elasticsearch.shield.authz.store;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.shield.SecurityContext;
import org.elasticsearch.shield.authz.RoleDescriptor;
import org.elasticsearch.shield.authz.permission.KibanaRole;
import org.elasticsearch.shield.authz.permission.KibanaUserRole;
import org.elasticsearch.shield.authz.permission.Role;
import org.elasticsearch.shield.authz.permission.SuperuserRole;
import org.elasticsearch.shield.authz.permission.TransportClientRole;
import org.elasticsearch.shield.user.KibanaUser;
import org.elasticsearch.shield.user.SystemUser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 *
 */
public class ReservedRolesStore implements RolesStore {

    private final SecurityContext securityContext;

    @Inject
    public ReservedRolesStore(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public Role role(String role) {
        switch (role) {
            case SuperuserRole.NAME:
                return SuperuserRole.INSTANCE;
            case TransportClientRole.NAME:
                return TransportClientRole.INSTANCE;
            case KibanaUserRole.NAME:
                return KibanaUserRole.INSTANCE;
            case KibanaRole.NAME:
                // The only user that should know about this role is the kibana user itself (who has this role). The reason we want to hide
                // this role is that it was created specifically for kibana, with all the permissions that the kibana user needs.
                // We don't want it to be assigned to other users.
                if (KibanaUser.is(securityContext.getUser())) {
                    return KibanaRole.INSTANCE;
                }
                return null;
            default:
                return null;
        }
    }

    public RoleDescriptor roleDescriptor(String role) {
        switch (role) {
            case SuperuserRole.NAME:
                return SuperuserRole.DESCRIPTOR;
            case TransportClientRole.NAME:
                return TransportClientRole.DESCRIPTOR;
            case KibanaUserRole.NAME:
                return KibanaUserRole.DESCRIPTOR;
            case KibanaRole.NAME:
                // The only user that should know about this role is the kibana user itself (who has this role). The reason we want to hide
                // this role is that it was created specifically for kibana, with all the permissions that the kibana user needs.
                // We don't want it to be assigned to other users.
                if (KibanaUser.is(securityContext.getUser())) {
                    return KibanaRole.DESCRIPTOR;
                }
                return null;
            default:
                return null;
        }
    }

    public Collection<RoleDescriptor> roleDescriptors() {
        if (KibanaUser.is(securityContext.getUser())) {
            return Arrays.asList(SuperuserRole.DESCRIPTOR, TransportClientRole.DESCRIPTOR, KibanaUserRole.DESCRIPTOR,
                    KibanaRole.DESCRIPTOR);
        }
        return Arrays.asList(SuperuserRole.DESCRIPTOR, TransportClientRole.DESCRIPTOR, KibanaUserRole.DESCRIPTOR);
    }

    public static Set<String> names() {
        return Sets.newHashSet(SuperuserRole.NAME, KibanaRole.NAME, TransportClientRole.NAME, KibanaUserRole.NAME);
    }

    public static boolean isReserved(String role) {
        switch (role) {
            case SuperuserRole.NAME:
            case KibanaRole.NAME:
            case KibanaUserRole.NAME:
            case TransportClientRole.NAME:
            case SystemUser.ROLE_NAME:
                return true;
            default:
                return false;
        }
    }
}
