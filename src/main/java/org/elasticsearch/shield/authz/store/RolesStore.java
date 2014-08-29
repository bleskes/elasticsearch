package org.elasticsearch.shield.authz.store;

import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public interface RolesStore {

    Permission.Global permission(String role);

    static interface Writable extends RolesStore {

        void set(String role, Privilege.Index privilege, String... indices);

        void grant(String role, Privilege.Index privilege, String... indices);

        void revoke(String role, Privilege.Index privileges, String... indices);

        void grant(String role, Privilege.Cluster privilege);

        void revoke(String role, Privilege.Cluster privileges);
    }

}
