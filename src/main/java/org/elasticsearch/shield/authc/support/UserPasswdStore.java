package org.elasticsearch.shield.authc.support;

/**
 *
 */
public interface UserPasswdStore {

    boolean verifyPassword(String username, char[] password);

    public static interface Writable extends UserPasswdStore {

        void store(String username, char[] password);

        void remove(String username);

    }

}