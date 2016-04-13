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

package org.elasticsearch.shield.authz.permission;

import org.elasticsearch.shield.authz.privilege.ClusterPrivilege;
import org.elasticsearch.shield.user.User;
import org.elasticsearch.transport.TransportRequest;

import java.util.List;
import java.util.function.Predicate;

/**
 * A permission that is based on privileges for cluster wide actions
 */
public interface ClusterPermission extends Permission {

    boolean check(String action, TransportRequest request, User user);

    public static class Core implements ClusterPermission {

        public static final Core NONE = new Core(ClusterPrivilege.NONE) {
            @Override
            public boolean check(String action, TransportRequest request, User user) {
                return false;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }
        };

        private final ClusterPrivilege privilege;
        private final Predicate<String> predicate;

        Core(ClusterPrivilege privilege) {
            this.privilege = privilege;
            this.predicate = privilege.predicate();
        }

        public ClusterPrivilege privilege() {
            return privilege;
        }

        @Override
        public boolean check(String action, TransportRequest request, User user) {
            return predicate.test(action);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    static class Globals implements ClusterPermission {

        private final List<GlobalPermission> globals;

        public Globals(List<GlobalPermission> globals) {
            this.globals = globals;
        }

        @Override
        public boolean check(String action, TransportRequest request, User user) {
            if (globals == null) {
                return false;
            }
            for (GlobalPermission global : globals) {
                if (global == null || global.cluster() == null) {
                    throw new RuntimeException();
                }
                if (global.cluster().check(action, request, user)) {
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
