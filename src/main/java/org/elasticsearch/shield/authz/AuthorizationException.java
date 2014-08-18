package org.elasticsearch.shield.authz;

import org.elasticsearch.rest.RestStatus;

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

    @Override
    public RestStatus status() {
        return RestStatus.UNAUTHORIZED;
    }
}
