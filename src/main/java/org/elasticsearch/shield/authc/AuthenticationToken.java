package org.elasticsearch.shield.authc;

/**
 *
 */
public interface AuthenticationToken {

    String principal();

    Object credentials();
}
