package org.elasticsearch.shield.authc;

import org.elasticsearch.rest.RestStatus;

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

    @Override
    public RestStatus status() {
        return RestStatus.UNAUTHORIZED;
    }
}
