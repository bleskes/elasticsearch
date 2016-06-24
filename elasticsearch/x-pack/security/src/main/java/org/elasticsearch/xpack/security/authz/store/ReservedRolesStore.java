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

package org.elasticsearch.xpack.security.authz.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.security.authz.permission.IngestAdminRole;
import org.elasticsearch.xpack.security.authz.permission.KibanaRole;
import org.elasticsearch.xpack.security.authz.permission.KibanaUserRole;
import org.elasticsearch.xpack.security.authz.permission.MonitoringUserRole;
import org.elasticsearch.xpack.security.authz.permission.RemoteMonitoringAgentRole;
import org.elasticsearch.xpack.security.authz.permission.Role;
import org.elasticsearch.xpack.security.authz.permission.SuperuserRole;
import org.elasticsearch.xpack.security.authz.permission.TransportClientRole;
import org.elasticsearch.xpack.security.user.KibanaUser;
import org.elasticsearch.xpack.security.user.SystemUser;

/**
 *
 */
public class ReservedRolesStore implements RolesStore {

    private final SecurityContext securityContext;

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
            case MonitoringUserRole.NAME:
                return MonitoringUserRole.INSTANCE;
            case RemoteMonitoringAgentRole.NAME:
                return RemoteMonitoringAgentRole.INSTANCE;
            case IngestAdminRole.NAME:
                return IngestAdminRole.INSTANCE;
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

    @Override
    public Map<String, Object> usageStats() {
        return Collections.emptyMap();
    }

    public RoleDescriptor roleDescriptor(String role) {
        switch (role) {
            case SuperuserRole.NAME:
                return SuperuserRole.DESCRIPTOR;
            case TransportClientRole.NAME:
                return TransportClientRole.DESCRIPTOR;
            case KibanaUserRole.NAME:
                return KibanaUserRole.DESCRIPTOR;
            case MonitoringUserRole.NAME:
                return MonitoringUserRole.DESCRIPTOR;
            case RemoteMonitoringAgentRole.NAME:
                return RemoteMonitoringAgentRole.DESCRIPTOR;
            case IngestAdminRole.NAME:
                return IngestAdminRole.DESCRIPTOR;
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
                    KibanaRole.DESCRIPTOR, MonitoringUserRole.DESCRIPTOR, RemoteMonitoringAgentRole.DESCRIPTOR,
                    IngestAdminRole.DESCRIPTOR);
        }
        return Arrays.asList(SuperuserRole.DESCRIPTOR, TransportClientRole.DESCRIPTOR, KibanaUserRole.DESCRIPTOR,
                MonitoringUserRole.DESCRIPTOR, RemoteMonitoringAgentRole.DESCRIPTOR, IngestAdminRole.DESCRIPTOR);
    }

    public static Set<String> names() {
        return Sets.newHashSet(SuperuserRole.NAME, KibanaRole.NAME, TransportClientRole.NAME, KibanaUserRole.NAME,
                MonitoringUserRole.NAME, RemoteMonitoringAgentRole.NAME, IngestAdminRole.NAME);
    }

    public static boolean isReserved(String role) {
        switch (role) {
            case SuperuserRole.NAME:
            case KibanaRole.NAME:
            case KibanaUserRole.NAME:
            case TransportClientRole.NAME:
            case MonitoringUserRole.NAME:
            case RemoteMonitoringAgentRole.NAME:
            case SystemUser.ROLE_NAME:
            case IngestAdminRole.NAME:
                return true;
            default:
                return false;
        }
    }
}
