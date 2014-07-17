package org.elasticsearch.shield.authz;

/**
 *
 */
public class AuthorizationException extends org.elasticsearch.shield.SecurityException {

    public AuthorizationException(String msg) {
        super(msg);
    }

    public AuthorizationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
