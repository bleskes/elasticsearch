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

import org.elasticsearch.shield.authz.permission.SuperuserRole;
import org.elasticsearch.shield.user.User.ReservedUser;

/**
 * XPack internal user that manages xpack. Has all cluster/indices permissions for watcher,
 * shield and monitoring to operate.
 */
public class XPackUser extends ReservedUser {

    public static final String NAME = "elastic";
    public static final String ROLE_NAME = SuperuserRole.NAME;
    public static final XPackUser INSTANCE = new XPackUser();

    XPackUser() {
        super(NAME, ROLE_NAME);
    }

    @Override
    public boolean equals(Object o) {
        return INSTANCE == o;
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
}
