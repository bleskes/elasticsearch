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

package org.elasticsearch.xpack.security.user;

import org.elasticsearch.Version;
import org.elasticsearch.xpack.security.authz.permission.KibanaRole;
import org.elasticsearch.xpack.security.authz.permission.LogstashSystemRole;
import org.elasticsearch.xpack.security.support.MetadataUtils;

/**
 * Built in user for logstash internals. Currently used for Logstash monitoring.
 */
public class LogstashSystemUser extends User {

    public static final String NAME = "logstash_system";
    public static final String ROLE_NAME = LogstashSystemRole.NAME;
    public static final Version DEFINED_SINCE = Version.V_5_2_0_UNRELEASED;

    public LogstashSystemUser(boolean enabled) {
        super(NAME, new String[]{ ROLE_NAME }, null, null, MetadataUtils.DEFAULT_RESERVED_METADATA, enabled);
    }
}
