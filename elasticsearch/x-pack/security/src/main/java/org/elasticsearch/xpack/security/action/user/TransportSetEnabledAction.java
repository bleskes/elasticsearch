/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.user.AnonymousUser;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.XPackUser;

/**
 * Transport action that handles setting a native or reserved user to enabled
 */
public class TransportSetEnabledAction extends HandledTransportAction<SetEnabledRequest, SetEnabledResponse> {

    private final NativeUsersStore usersStore;

    @Inject
    public TransportSetEnabledAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                        ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                        NativeUsersStore usersStore) {
        super(settings, SetEnabledAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                SetEnabledRequest::new);
        this.usersStore = usersStore;
    }

    @Override
    protected void doExecute(SetEnabledRequest request, ActionListener<SetEnabledResponse> listener) {
        final String username = request.username();
        // make sure the user is not disabling themselves
        if (Authentication.getAuthentication(threadPool.getThreadContext()).getRunAsUser().principal().equals(request.username())) {
            listener.onFailure(new IllegalArgumentException("users may not update the enabled status of their own account"));
            return;
        } else if (SystemUser.NAME.equals(username) || XPackUser.NAME.equals(username)) {
            listener.onFailure(new IllegalArgumentException("user [" + username + "] is internal"));
            return;
        } else if (AnonymousUser.isAnonymousUsername(username, settings)) {
            listener.onFailure(new IllegalArgumentException("user [" + username + "] is anonymous and cannot be modified using the api"));
            return;
        }

        usersStore.setEnabled(username, request.enabled(), request.getRefreshPolicy(), new ActionListener<Void>() {
            @Override
            public void onResponse(Void v) {
                listener.onResponse(new SetEnabledResponse());
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
