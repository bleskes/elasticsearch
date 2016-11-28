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
import org.elasticsearch.xpack.common.GroupedActionListener;
import org.elasticsearch.xpack.monitoring.collector.Collector;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.user.XPackUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.elasticsearch.common.Strings.arrayToDelimitedString;

public class TransportGetUsersAction extends HandledTransportAction<GetUsersRequest, GetUsersResponse> {

    private final NativeUsersStore usersStore;
    private final ReservedRealm reservedRealm;

    @Inject
    public TransportGetUsersAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                   IndexNameExpressionResolver indexNameExpressionResolver, NativeUsersStore usersStore,
                                   TransportService transportService, ReservedRealm reservedRealm) {
        super(settings, GetUsersAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                GetUsersRequest::new);
        this.usersStore = usersStore;
        this.reservedRealm = reservedRealm;
    }

    @Override
    protected void doExecute(final GetUsersRequest request, final ActionListener<GetUsersResponse> listener) {
        final String[] requestedUsers = request.usernames();
        final boolean specificUsersRequested = requestedUsers != null && requestedUsers.length > 0;
        final List<String> usersToSearchFor = new ArrayList<>();
        final List<User> users = new ArrayList<>();
        final List<String> realmLookup = new ArrayList<>();
        if (specificUsersRequested) {
            for (String username : requestedUsers) {
                if (ReservedRealm.isReserved(username, settings)) {
                    realmLookup.add(username);
                } else if (SystemUser.NAME.equals(username) || XPackUser.NAME.equals(username)) {
                    listener.onFailure(new IllegalArgumentException("user [" + username + "] is internal"));
                    return;
                } else {
                    usersToSearchFor.add(username);
                }
            }
        }

        final ActionListener<Collection<Collection<User>>> sendingListener = ActionListener.wrap((userLists) -> {
                users.addAll(userLists.stream().flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList()));
                listener.onResponse(new GetUsersResponse(users));
            }, listener::onFailure);
        final GroupedActionListener<Collection<User>> groupListener =
                new GroupedActionListener<>(sendingListener, 2, Collections.emptyList());
        // We have two sources for the users object, the reservedRealm and the usersStore, we query both at the same time with a
        // GroupedActionListener
        if (realmLookup.isEmpty()) {
            if (specificUsersRequested == false) {
                // we get all users from the realm
                reservedRealm.users(groupListener);
            } else {
                groupListener.onResponse(Collections.emptyList());// pass an empty list to inform the group listener
                // - no real lookups necessary
            }
        } else {
            // nested group listener action here - for each of the users we got and fetch it concurrently - once we are done we notify
            // the "global" group listener.
            GroupedActionListener<User> realmGroupListener = new GroupedActionListener<>(groupListener, realmLookup.size(),
                    Collections.emptyList());
            for (String user : realmLookup) {
                reservedRealm.lookupUser(user, realmGroupListener);
            }
        }

        // user store lookups
        if (specificUsersRequested && usersToSearchFor.isEmpty()) {
            groupListener.onResponse(Collections.emptyList()); // no users requested notify
        } else {
            // go and get all users from the users store and pass it directly on to the group listener
            usersStore.getUsers(usersToSearchFor.toArray(new String[usersToSearchFor.size()]), groupListener);
        }
    }
}
