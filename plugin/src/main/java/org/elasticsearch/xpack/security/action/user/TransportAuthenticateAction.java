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

package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.user.XPackUser;

/**
 */
public class TransportAuthenticateAction extends HandledTransportAction<AuthenticateRequest, AuthenticateResponse> {

    private final SecurityContext securityContext;

    @Inject
    public TransportAuthenticateAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                       ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                       SecurityContext securityContext) {
        super(settings, AuthenticateAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                AuthenticateRequest::new);
        this.securityContext = securityContext;
    }

    @Override
    protected void doExecute(AuthenticateRequest request, ActionListener<AuthenticateResponse> listener) {
        final User runAsUser = securityContext.getUser();
        final User authUser = runAsUser == null ? null : runAsUser.authenticatedUser();
        if (authUser == null) {
            listener.onFailure(new ElasticsearchSecurityException("did not find an authenticated user"));
        } else if (SystemUser.is(authUser) || XPackUser.is(authUser)) {
            listener.onFailure(new IllegalArgumentException("user [" + authUser.principal() + "] is internal"));
        } else if (SystemUser.is(runAsUser) || XPackUser.is(runAsUser)) {
            listener.onFailure(new IllegalArgumentException("user [" + runAsUser.principal() + "] is internal"));
        } else {
            listener.onResponse(new AuthenticateResponse(runAsUser));
        }
    }
}
