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

package org.elasticsearch.shield.transport;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationException;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authz.AuthorizationService;
import org.elasticsearch.transport.TransportRequest;

/**
 * This interface allows to intercept messages as they come in and execute logic
 * This is used in SHIELD to execute the authentication/authorization on incoming
 * messages.
 * Note that this filter only applies for nodes, but not for clients.
 */
public interface ServerTransportFilter {

    /**
     * Called just after the given request was received by the transport. Any exception
     * thrown by this method will stop the request from being handled and the error will
     * be sent back to the sender.
     */
    void inbound(String action, TransportRequest request);

    /**
     * The server trasnport filter that should be used in nodes as it ensures that an incoming
     * request is properly authenticated and authorized
     */
    public static class NodeProfile implements ServerTransportFilter {

        private final AuthenticationService authcService;
        private final AuthorizationService authzService;

        @Inject
        public NodeProfile(AuthenticationService authcService, AuthorizationService authzService) {
            this.authcService = authcService;
            this.authzService = authzService;
        }

        @Override
        public void inbound(String action, TransportRequest request) {
            /**
                here we don't have a fallback user, as all incoming request are
                expected to have a user attached (either in headers or in context)
                We can make this assumption because in nodes we also have the
                {@link ClientTransportFilter.Node} that makes sure all outgoing requsts
                from all the nodes are attached with a user (either a serialize user
                an authentication token
             */
            User user = authcService.authenticate(action, request, null);
            authzService.authorize(user, action, request);
        }
    }

    /**
     * A server transport filter rejects internal calls, which should be used on connections
     * where only clients connect to. This ensures that no client can send any internal actions
     * or shard level actions. As it extends the NodeProfile the authentication/authorization is
     * done as well
     */
    public static class ClientProfile extends NodeProfile {

        @Inject
        public ClientProfile(AuthenticationService authcService, AuthorizationService authzService) {
            super(authcService, authzService);
        }

        @Override
        public void inbound(String action, TransportRequest request) {
            // TODO is ']' sufficient to mark as shard action?
            boolean isInternalOrShardAction = action.startsWith("internal:") || action.endsWith("]");
            if (isInternalOrShardAction) {
                throw new AuthenticationException("executing internal/shard actions is considered malicious and forbidden");
            }
            super.inbound(action, request);
        }
    }

}
