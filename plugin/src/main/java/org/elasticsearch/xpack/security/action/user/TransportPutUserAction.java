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

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.user.AnonymousUser;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.XPackUser;

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
        final String username = request.username();
        if (ReservedRealm.isReserved(username, settings)) {
            if (AnonymousUser.isAnonymousUsername(username, settings)) {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is anonymous and cannot be modified via the API"));
                return;
            } else {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is reserved and only the " +
                        "password can be changed"));
                return;
            }
        } else if (SystemUser.NAME.equals(username) || XPackUser.NAME.equals(username)) {
            listener.onFailure(new IllegalArgumentException("user [" + username + "] is internal"));
            return;
        }

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
            public void onFailure(Exception e) {
                logger.error((Supplier<?>) () -> new ParameterizedMessage("failed to put user [{}]", request.username()), e);
                listener.onFailure(e);
            }
        });
    }
}
