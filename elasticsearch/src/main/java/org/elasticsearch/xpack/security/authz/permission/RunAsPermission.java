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

import org.elasticsearch.xpack.security.authz.privilege.GeneralPrivilege;

import java.util.List;
import java.util.function.Predicate;

/**
 * A permissions that is based on a general privilege that contains patterns of users that this
 * user can execute a request as
 */
public interface RunAsPermission extends Permission {

    /**
     * Checks if this permission grants run as to the specified user
     */
    boolean check(String username);

    class Core implements RunAsPermission {

        public static final Core NONE = new Core(GeneralPrivilege.NONE);

        private final GeneralPrivilege privilege;
        private final Predicate<String> predicate;

        public Core(GeneralPrivilege privilege) {
            this.privilege = privilege;
            this.predicate = privilege.predicate();
        }

        @Override
        public boolean check(String username) {
            return predicate.test(username);
        }

        @Override
        public boolean isEmpty() {
            return this == NONE;
        }
    }

    class Globals implements RunAsPermission {
        private final List<GlobalPermission> globals;

        public Globals(List<GlobalPermission> globals) {
            this.globals = globals;
        }

        @Override
        public boolean check(String username) {
            if (globals == null) {
                return false;
            }
            for (GlobalPermission global : globals) {
                if (global.runAs().check(username)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            if (globals == null || globals.isEmpty()) {
                return true;
            }
            for (GlobalPermission global : globals) {
                if (!global.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }
}
