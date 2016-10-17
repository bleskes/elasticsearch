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

import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authz.privilege.ClusterPrivilege;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A permission that is based on privileges for cluster wide actions
 */
public interface ClusterPermission extends Permission {

    boolean check(String action, TransportRequest request, Authentication authentication);

    class Core implements ClusterPermission {

        public static final Core NONE = new Core(ClusterPrivilege.NONE) {
            @Override
            public boolean check(String action, TransportRequest request, Authentication authentication) {
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
        public boolean check(String action, TransportRequest request, Authentication authentication) {
            return predicate.test(action);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    class Globals implements ClusterPermission {

        private final List<GlobalPermission> globals;

        Globals(List<GlobalPermission> globals) {
            this.globals = globals;
        }

        @Override
        public boolean check(String action, TransportRequest request, Authentication authentication) {
            if (globals == null) {
                return false;
            }
            for (GlobalPermission global : globals) {
                Objects.requireNonNull(global, "global must not be null");
                Objects.requireNonNull(global.indices(), "global.indices() must not be null");
                if (global.cluster().check(action, request, authentication)) {
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
