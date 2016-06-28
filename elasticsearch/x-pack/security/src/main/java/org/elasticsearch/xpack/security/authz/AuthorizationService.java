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

package org.elasticsearch.xpack.security.authz;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.transport.TransportRequest;

import java.util.List;

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
    List<String> authorizedIndicesAndAliases(User user, String action);

    /**
     * Verifies that the given user can execute the given request (and action). If the user doesn't
     * have the appropriate privileges for this action/request, an {@link ElasticsearchSecurityException}
     * will be thrown.
     *
     * @param authentication  The authentication information
     * @param action          The action
     * @param request         The request
     * @throws ElasticsearchSecurityException   If the given user is no allowed to execute the given request
     */
    void authorize(Authentication authentication, String action, TransportRequest request) throws ElasticsearchSecurityException;

}
