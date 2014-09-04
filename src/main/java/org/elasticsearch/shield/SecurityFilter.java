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

package org.elasticsearch.shield;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.authc.system.SystemRealm;
import org.elasticsearch.shield.authz.AuthorizationService;
import org.elasticsearch.shield.transport.TransportFilter;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public class SecurityFilter extends AbstractComponent {

    private final AuthenticationService authcService;
    private final AuthorizationService authzService;

    @Inject
    public SecurityFilter(Settings settings, AuthenticationService authcService, AuthorizationService authzService, RestController restController) {
        super(settings);
        this.authcService = authcService;
        this.authzService = authzService;
        restController.registerFilter(new Rest(this));
    }

    void process(String action, TransportRequest request) {
        AuthenticationToken token = authcService.token(action, request, SystemRealm.TOKEN);
        User user = authcService.authenticate(action, request, token);
        authzService.authorize(user, action, request);
    }

    public static class Rest extends RestFilter {

        static {
            BaseRestHandler.addUsefulHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
        }

        private final SecurityFilter filter;

        public Rest(SecurityFilter filter) {
            this.filter = filter;
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE;
        }

        @Override
        public void process(RestRequest request, RestChannel channel, RestFilterChain filterChain) throws Exception {
            filter.authcService.verifyToken(request);
            filterChain.continueProcessing(request, channel);
        }
    }

    public static class Transport extends TransportFilter.Base {

        private final SecurityFilter filter;

        @Inject
        public Transport(SecurityFilter filter) {
            this.filter = filter;
        }

        @Override
        public void inboundRequest(String action, TransportRequest request) {
            filter.process(action, request);
        }
    }

    public static class Action implements org.elasticsearch.action.support.ActionFilter {

        private final SecurityFilter filter;

        @Inject
        public Action(SecurityFilter filter) {
            this.filter = filter;
        }

        @Override
        public void process(String action, ActionRequest request, ActionListener listener, ActionFilterChain chain) {
            try {
                filter.process(action, request);
            } catch (Throwable t) {
                listener.onFailure(t);
                return;
            }
            chain.continueProcessing(action, request, listener);
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE;
        }
    }
}