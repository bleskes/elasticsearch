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
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.esnative.NativeUsersStore;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;

public class TransportGetUsersAction extends HandledTransportAction<GetUsersRequest, GetUsersResponse> {

    private final NativeUsersStore usersStore;

    @Inject
    public TransportGetUsersAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                   IndexNameExpressionResolver indexNameExpressionResolver, NativeUsersStore usersStore,
                                   TransportService transportService) {
        super(settings, GetUsersAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                GetUsersRequest::new);
        this.usersStore = usersStore;
    }

    @Override
    protected void doExecute(final GetUsersRequest request, final ActionListener<GetUsersResponse> listener) {
        if (request.usernames().length == 1) {
            final String username = request.usernames()[0];
            // We can fetch a single user with a get, much cheaper:
            usersStore.getUser(username, new ActionListener<User>() {
                @Override
                public void onResponse(User user) {
                    if (user == null) {
                        listener.onResponse(new GetUsersResponse());
                    } else {
                        listener.onResponse(new GetUsersResponse(user));
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    logger.error("failed to retrieve user [{}]", e, username);
                    listener.onFailure(e);
                }
            });
        } else {
            usersStore.getUsers(request.usernames(), new ActionListener<List<User>>() {
                @Override
                public void onResponse(List<User> users) {
                    listener.onResponse(new GetUsersResponse(users));
                }

                @Override
                public void onFailure(Throwable e) {
                    logger.error("failed to retrieve user [{}]", e,
                            Strings.arrayToDelimitedString(request.usernames(), ","));
                    listener.onFailure(e);
                }
            });
        }
    }
}
