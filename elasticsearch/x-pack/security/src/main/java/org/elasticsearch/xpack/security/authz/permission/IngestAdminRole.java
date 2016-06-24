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
import org.elasticsearch.xpack.security.authz.privilege.ClusterPrivilege;
import org.elasticsearch.xpack.security.authz.privilege.Privilege.Name;

/**
 * Role for users that should be allowed to use the Add Data/Ingest features in the UI
 */
public class IngestAdminRole extends Role {

    private static final String[] CLUSTER_PRIVILEGES = new String[] { "manage_index_templates", "manage_pipeline" };
    private static final RoleDescriptor.IndicesPrivileges[] INDICES_PRIVILEGES = new RoleDescriptor.IndicesPrivileges[0];

    public static final String NAME = "ingest_admin";
    public static final RoleDescriptor DESCRIPTOR = new RoleDescriptor(NAME, CLUSTER_PRIVILEGES, INDICES_PRIVILEGES, null);
    public static final IngestAdminRole INSTANCE = new IngestAdminRole();

    private IngestAdminRole() {
        super(DESCRIPTOR.getName(),
                new ClusterPermission.Core(ClusterPrivilege.get(new Name(DESCRIPTOR.getClusterPrivileges()))),
                new IndicesPermission.Core(Role.Builder.convertFromIndicesPrivileges(DESCRIPTOR.getIndicesPrivileges())),
                RunAsPermission.Core.NONE);
    }
}
