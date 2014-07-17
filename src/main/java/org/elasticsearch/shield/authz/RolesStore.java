package org.elasticsearch.shield.authz;

/**
 *
 */
public interface RolesStore {

    Permission permission(String... roles);

    public static interface Writable extends RolesStore {

        void set(String role, Privilege.Index[] privileges, String[] indices);

        void grant(String role, Privilege.Index[] privileges, String[] indices);

        void grant(String role, Privilege.Cluster[] privileges);

        void revoke(String role, Privilege.Index[] privileges, String[] indices);

        void revoke(String role, Privilege.Cluster[] privileges);
    }

}
