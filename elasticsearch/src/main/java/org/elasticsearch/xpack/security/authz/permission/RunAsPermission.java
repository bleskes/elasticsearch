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

import org.elasticsearch.xpack.security.authz.privilege.Privilege;

import java.util.function.Predicate;

/**
 * A permissions that is based on a general privilege that contains patterns of users that this
 * user can execute a request as
 */
public final class RunAsPermission {

    public static final RunAsPermission NONE = new RunAsPermission(Privilege.NONE);

    private final Predicate<String> predicate;

    RunAsPermission(Privilege privilege) {
        this.predicate = privilege.predicate();
    }

    /**
     * Checks if this permission grants run as to the specified user
     */
    public boolean check(String username) {
        return predicate.test(username);
    }
}
