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

import org.elasticsearch.xpack.security.authz.privilege.ClusterPrivilege;

import java.util.function.Predicate;

/**
 * A permission that is based on privileges for cluster wide actions
 */
public final class ClusterPermission {

    public static final ClusterPermission NONE = new ClusterPermission(ClusterPrivilege.NONE);

    private final ClusterPrivilege privilege;
    private final Predicate<String> predicate;

    ClusterPermission(ClusterPrivilege privilege) {
        this.privilege = privilege;
        this.predicate = privilege.predicate();
    }

    public ClusterPrivilege privilege() {
        return privilege;
    }

    public boolean check(String action) {
        return predicate.test(action);
    }
}
