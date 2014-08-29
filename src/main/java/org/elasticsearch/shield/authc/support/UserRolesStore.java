package org.elasticsearch.shield.authc.support;

/**
 *
 */
public interface UserRolesStore {

    String[] roles(String username);

    static interface Writable extends UserRolesStore {

        void setRoles(String username, String... roles);

        void addRoles(String username, String... roles);

        void removeRoles(String username, String... roles);

        void removeUser(String username);
    }
}
