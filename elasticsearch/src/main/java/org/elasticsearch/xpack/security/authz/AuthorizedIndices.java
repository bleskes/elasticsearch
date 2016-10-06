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

package org.elasticsearch.xpack.security.authz;

import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.xpack.security.SecurityTemplateService;
import org.elasticsearch.xpack.security.authz.permission.Role;
import org.elasticsearch.xpack.security.authz.permission.SuperuserRole;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.user.XPackUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Abstraction used to make sure that we lazily load authorized indices only when requested and only maximum once per request. Also
 * makes sure that authorized indices don't get updated throughout the same request for the same user.
 */
class AuthorizedIndices {
    private final User user;
    private final String action;
    private final MetaData metaData;
    private final Collection<Role> userRoles;
    private List<String> authorizedIndices;

    AuthorizedIndices(User user, Collection<Role> userRoles, String action, MetaData metaData) {
        this.user = user;
        this.userRoles = userRoles;
        this.action = action;
        this.metaData = metaData;
    }

    List<String> get() {
        if (authorizedIndices == null) {
            authorizedIndices = load();
        }
        return authorizedIndices;
    }

    private List<String> load() {
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Predicate<String>> predicates = new ArrayList<>();
        for (Role userRole : userRoles) {
            predicates.add(userRole.indices().allowedIndicesMatcher(action));
        }

        Predicate<String> predicate = predicates.stream().reduce(s -> false, Predicate::or);

        List<String> indicesAndAliases = new ArrayList<>();
        // TODO: can this be done smarter? I think there are usually more indices/aliases in the cluster then indices defined a roles?
        for (Map.Entry<String, AliasOrIndex> entry : metaData.getAliasAndIndexLookup().entrySet()) {
            String aliasOrIndex = entry.getKey();
            if (predicate.test(aliasOrIndex)) {
                indicesAndAliases.add(aliasOrIndex);
            }
        }

        if (XPackUser.is(user) == false && Arrays.binarySearch(user.roles(), SuperuserRole.NAME) < 0) {
            // we should filter out the .security index from wildcards
            indicesAndAliases.remove(SecurityTemplateService.SECURITY_INDEX_NAME);
        }
        return Collections.unmodifiableList(indicesAndAliases);
    }
}
