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

package org.elasticsearch.xpack.security.authz.permission;

import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.security.authz.permission.ClusterPermission.Core;
import org.elasticsearch.xpack.security.support.MetadataUtils;

/**
 * A built-in role that grants users the necessary privileges to use Monitoring. The user will also need the {@link KibanaUserRole}
 */
public class MonitoringUserRole extends Role {

    private static final RoleDescriptor.IndicesPrivileges[] INDICES_PRIVILEGES = new RoleDescriptor.IndicesPrivileges[] {
            RoleDescriptor.IndicesPrivileges.builder()
                    .indices(".marvel-es-*", ".monitoring-*")
                    .privileges("read")
                    .build() };

    public static final String NAME = "monitoring_user";
    public static final RoleDescriptor DESCRIPTOR =
            new RoleDescriptor(NAME, null, INDICES_PRIVILEGES, null, MetadataUtils.DEFAULT_RESERVED_METADATA);
    public static final MonitoringUserRole INSTANCE = new MonitoringUserRole();

    private MonitoringUserRole() {
        super(DESCRIPTOR.getName(),
                Core.NONE,
                new IndicesPermission.Core(Role.Builder.convertFromIndicesPrivileges(DESCRIPTOR.getIndicesPrivileges())),
                RunAsPermission.Core.NONE);
    }
}
