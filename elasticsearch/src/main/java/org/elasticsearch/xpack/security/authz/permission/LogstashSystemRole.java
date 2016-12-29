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

import org.elasticsearch.xpack.monitoring.action.MonitoringBulkAction;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.security.authz.privilege.ClusterPrivilege;
import org.elasticsearch.xpack.security.authz.privilege.Privilege.Name;
import org.elasticsearch.xpack.security.support.MetadataUtils;

public class LogstashSystemRole extends Role {

    private static final String[] CLUSTER_PRIVILEGES = new String[] { "monitor", MonitoringBulkAction.NAME};

    public static final String NAME = "logstash_system";
    public static final RoleDescriptor DESCRIPTOR =
            new RoleDescriptor(NAME, CLUSTER_PRIVILEGES, null, null, MetadataUtils.DEFAULT_RESERVED_METADATA);
    public static final LogstashSystemRole INSTANCE = new LogstashSystemRole();

    private LogstashSystemRole() {
        super(DESCRIPTOR.getName(),
              new ClusterPermission.Core(ClusterPrivilege.get(new Name(DESCRIPTOR.getClusterPrivileges()))),
              new IndicesPermission.Core(Builder.convertFromIndicesPrivileges(DESCRIPTOR.getIndicesPrivileges())),
              RunAsPermission.Core.NONE);
    }
}
