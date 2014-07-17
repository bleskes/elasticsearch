package org.elasticsearch.shield.authc;

/**
 *
 */
public class AuthenticationException extends org.elasticsearch.shield.SecurityException {

    public AuthenticationException(String msg) {
        super(msg);
    }

    public AuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
