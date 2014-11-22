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

package org.elasticsearch.shield.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.audit.AuditTrail;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authz.AuthorizationException;
import org.elasticsearch.shield.authz.AuthorizationService;
import org.elasticsearch.shield.key.KeyService;
import org.elasticsearch.shield.key.SignatureException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ShieldActionFilter implements ActionFilter {

    private final AuthenticationService authcService;
    private final AuthorizationService authzService;
    private final KeyService keyService;
    private final AuditTrail auditTrail;

    @Inject
    public ShieldActionFilter(AuthenticationService authcService, AuthorizationService authzService, KeyService keyService, AuditTrail auditTrail) {
        this.authcService = authcService;
        this.authzService = authzService;
        this.keyService = keyService;
        this.auditTrail = auditTrail;
    }

    @Override
    public void apply(String action, ActionRequest request, ActionListener listener, ActionFilterChain chain) {
        try {
            /**
             here we fallback on the system user. Internal system requests are requests that are triggered by
             the system itself (e.g. pings, update mappings, share relocation, etc...) and were not originated
             by user interaction. Since these requests are triggered by es core modules, they are security
             agnostic and therefore not associated with any user. When these requests execute locally, they
             are executed directly on their relevant action. Since there is no other way a request can make
             it to the action without an associated user (not via REST or transport - this is taken care of by
             the {@link Rest} filter and the {@link ServerTransport} filter respectively), it's safe to assume a system user
             here if a request is not associated with any other user.
             */
            User user = authcService.authenticate(action, request, User.SYSTEM);
            authzService.authorize(user, action, request);
            request = unsign(user, action, request);
            chain.proceed(action, request, new SigningListener(this, listener));
        } catch (Throwable t) {
            listener.onFailure(t);
        }
    }

    @Override
    public void apply(String action, ActionResponse response, ActionListener listener, ActionFilterChain chain) {
        chain.proceed(action, response, listener);
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    <Request extends ActionRequest> Request unsign(User user, String action, Request request) {

        try {

            if (request instanceof SearchScrollRequest) {
                SearchScrollRequest scrollRequest = (SearchScrollRequest) request;
                String scrollId = scrollRequest.scrollId();
                scrollRequest.scrollId(keyService.unsignAndVerify(scrollId));
                return request;
            }

            if (request instanceof ClearScrollRequest) {
                ClearScrollRequest clearScrollRequest = (ClearScrollRequest) request;
                List<String> signedIds = clearScrollRequest.scrollIds();
                List<String> unsignedIds = new ArrayList<>(signedIds.size());
                for (String signedId : signedIds) {
                    unsignedIds.add(keyService.unsignAndVerify(signedId));
                }
                clearScrollRequest.scrollIds(unsignedIds);
                return request;
            }

            return request;

        } catch (SignatureException se) {
            auditTrail.tamperedRequest(user, action, request);
            throw new AuthorizationException("Invalid request: " + se.getMessage());
        }
    }

    <Response extends ActionResponse> Response sign(Response response) {

        if (response instanceof SearchResponse) {
            SearchResponse searchResponse = (SearchResponse) response;
            String scrollId = searchResponse.getScrollId();
            if (scrollId != null && !keyService.signed(scrollId)) {
                searchResponse.scrollId(keyService.sign(scrollId));
            }
            return response;
        }

        return response;
    }

    static class SigningListener<Response extends ActionResponse> implements ActionListener<Response> {

        private final ShieldActionFilter filter;
        private final ActionListener innerListener;

        private SigningListener(ShieldActionFilter filter, ActionListener innerListener) {
            this.filter = filter;
            this.innerListener = innerListener;
        }

        @Override @SuppressWarnings("unchecked")
        public void onResponse(Response response) {
            response = this.filter.sign(response);
            innerListener.onResponse(response);
        }

        @Override
        public void onFailure(Throwable e) {
            innerListener.onFailure(e);
        }
    }
}
