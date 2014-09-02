/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.shield.authc;

import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.transport.TransportMessage;

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

    boolean hasToken(RestRequest request);

    /**
     * Attempts to extract a authentication token from the request. If an appropriate token is found
     * {@link #authenticate(AuthenticationToken)} will be called for an authentication attempt. If no
     * appropriate token is found, {@code null} is returned.
     *
     * @param message   The request
     * @return          The authentication token this realm can authenticate, {@code null} if no such
     *                  token is found
     */
    T token(TransportMessage<?> message);

    /**
     * @return  {@code true} if this realm supports the given authentication token, {@code false} otherwise.
     */
    boolean supports(AuthenticationToken token);

    /**
     * Authenticates the given token. A successful authentication will return the User associated
     * with the given token. An unsuccessful authentication returns {@code null}.
     *
     * @param token The authentication token
     * @return      The authenticated user or {@code null} if authentication failed.
     */
    User authenticate(T token);

}
