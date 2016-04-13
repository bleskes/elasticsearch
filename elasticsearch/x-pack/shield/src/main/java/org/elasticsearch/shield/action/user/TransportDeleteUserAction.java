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
import org.elasticsearch.shield.authc.esnative.ReservedRealm;
import org.elasticsearch.shield.user.AnonymousUser;
import org.elasticsearch.shield.user.SystemUser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportDeleteUserAction extends HandledTransportAction<DeleteUserRequest, DeleteUserResponse> {

    private final NativeUsersStore usersStore;

    @Inject
    public TransportDeleteUserAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                     IndexNameExpressionResolver indexNameExpressionResolver, NativeUsersStore usersStore,
                                     TransportService transportService) {
        super(settings, DeleteUserAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                DeleteUserRequest::new);
        this.usersStore = usersStore;
    }

    @Override
    protected void doExecute(DeleteUserRequest request, final ActionListener<DeleteUserResponse> listener) {
        final String username = request.username();
        if (ReservedRealm.isReserved(username)) {
            if (AnonymousUser.isAnonymousUsername(username)) {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is anonymous and cannot be deleted"));
                return;
            } else {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is reserved and cannot be deleted"));
                return;
            }
        } else if (SystemUser.NAME.equals(username)) {
            listener.onFailure(new IllegalArgumentException("user [" + username + "] is internal"));
            return;
        }

        usersStore.deleteUser(request, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean found) {
                listener.onResponse(new DeleteUserResponse(found));
            }

            @Override
            public void onFailure(Throwable e) {
                listener.onFailure(e);
            }
        });
    }
}
