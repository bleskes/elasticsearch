package org.elasticsearch.shield.authc;

import org.elasticsearch.shield.User;
import org.elasticsearch.transport.TransportRequest;

/**
 * An authentication mechanism to which the default authentication {@link org.elasticsearch.shield.authc.AuthenticationService service}
 * delegates the authentication process. Different realms may be defined, each may be based on different
 * authentication mechanism supporting its own specific authentication token type.
 */
public interface Realm<T extends AuthenticationToken> {

    /**
     * @return The type of this realm
     */
    String type();

    /**
     * Attempts to extract a authentication token from the request. If an appropriate token is found
     * {@link #authenticate(AuthenticationToken)} will be called for an authentication attempt. If no
     * appropriate token is found, {@code null} is returned.
     *
     * @param request   The request
     * @return          The authentication token this realm can authenticate, {@code null} if no such
     *                  token is found
     */
    T token(TransportRequest request);

    /**
     * Authenticates the given token. A successful authentication will return the User associated
     * with the given token. An unsuccessful authentication returns {@code null}.
     *
     * @param token The authentication token
     * @return      The authenticated user or {@code null} if authentication failed.
     */
    User authenticate(T token);

}
