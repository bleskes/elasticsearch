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

package org.elasticsearch.shield.user;

import org.elasticsearch.shield.authz.privilege.SystemPrivilege;

import java.util.function.Predicate;

/**
 * Shield internal user that manages the {@code .shield}
 * index. Has permission to monitor the cluster as well as all actions that deal
 * with the shield admin index.
 */
public class SystemUser extends User {

    public static final String NAME = "_system";
    public static final String ROLE_NAME = "_system";

    public static final User INSTANCE = new SystemUser();

    private static final Predicate<String> PREDICATE = SystemPrivilege.INSTANCE.predicate();

    private SystemUser() {
        super(NAME, ROLE_NAME);
    }

    @Override
    public boolean equals(Object o) {
        return o == INSTANCE;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public static boolean is(User user) {
        return INSTANCE.equals(user);
    }

    public static boolean is(String principal) {
        return NAME.equals(principal);
    }

    public static boolean isAuthorized(String action) {
        return PREDICATE.test(action);
    }
}
