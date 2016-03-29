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

package org.elasticsearch.shield.action.user;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.esnative.NativeUsersStore;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportPutUserAction extends HandledTransportAction<PutUserRequest, PutUserResponse> {

    private final NativeUsersStore usersStore;

    @Inject
    public TransportPutUserAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                  IndexNameExpressionResolver indexNameExpressionResolver,
                                  NativeUsersStore usersStore, TransportService transportService) {
        super(settings, PutUserAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, PutUserRequest::new);
        this.usersStore = usersStore;
    }

    @Override
    protected void doExecute(final PutUserRequest request, final ActionListener<PutUserResponse> listener) {
        usersStore.putUser(request, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean created) {
                if (created) {
                    logger.info("added user [{}]", request.username());
                } else {
                    logger.info("updated user [{}]", request.username());
                }
                listener.onResponse(new PutUserResponse(created));
            }

            @Override
            public void onFailure(Throwable e) {
                logger.error("failed to put user [{}]", e, request.username());
                listener.onFailure(e);
            }
        });
    }
}
