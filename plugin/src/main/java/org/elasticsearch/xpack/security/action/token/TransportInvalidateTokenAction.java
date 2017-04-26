/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.token;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.authc.TokenService;

/**
 * Transport action responsible for handling invalidation of tokens
 */
public final class TransportInvalidateTokenAction extends HandledTransportAction<InvalidateTokenRequest, InvalidateTokenResponse> {

    private final TokenService tokenService;

    @Inject
    public TransportInvalidateTokenAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                          ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                          TokenService tokenService) {
        super(settings, InvalidateTokenAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, InvalidateTokenRequest::new);
        this.tokenService = tokenService;
    }

    @Override
    protected void doExecute(InvalidateTokenRequest request,
                             ActionListener<InvalidateTokenResponse> listener) {
        tokenService.invalidateToken(request.getTokenString(), ActionListener.wrap(
                created -> listener.onResponse(new InvalidateTokenResponse(created)),
                listener::onFailure));
    }
}
