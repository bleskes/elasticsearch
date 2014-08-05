package org.elasticsearch.shield.authz;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.shield.User;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public interface AuthorizationService {

    /**
     * Returns all indices and aliases the given user is allowed to execute the given action on.
     *
     * @param user      The user
     * @param action    The action
     */
    ImmutableList<String> authorizedIndicesAndAliases(User user, String action);

    /**
     * Verifies that the given user can execute the given request (and action). If the user doesn't
     * have the appropriate privileges for this action/request, an {@link AuthorizationException}
     * will be thrown.
     *
     * @param user      The user
     * @param action    The action
     * @param request   The request
     * @throws AuthorizationException   If the given user is no allowed to execute the given request
     */
    void authorize(User user, String action, TransportRequest request) throws AuthorizationException;

}
