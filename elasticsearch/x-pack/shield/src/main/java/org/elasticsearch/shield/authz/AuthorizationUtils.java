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

package org.elasticsearch.shield.authz;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.shield.SystemUser;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.InternalAuthenticationService;
import org.elasticsearch.shield.support.AutomatonPredicate;
import org.elasticsearch.shield.support.Automatons;

import java.util.function.Predicate;

/**
 *
 */
public final class AuthorizationUtils {

    private static final Predicate<String> INTERNAL_PREDICATE = new AutomatonPredicate(Automatons.patterns("internal:*"));

    private AuthorizationUtils() {}

    /**
     * This method is used to determine if a request should be executed as the system user, even if the request already
     * has a user associated with it.
     *
     * In order for the system user to be used, one of the following conditions must be true:
     *
     * <ul>
     *     <li>the action is an internal action and no user is associated with the request</li>
     *     <li>the action is an internal action and the system user is already associated with the request</li>
     *     <li>the action is an internal action and the thread context contains a non-internal action as the originating action</li>
     * </ul>
     *
     * @param threadContext the {@link ThreadContext} that contains the headers and context associated with the request
     * @param action the action name that is being executed
     * @return true if the system user should be used to execute a request
     */
    public static boolean shouldReplaceUserWithSystem(ThreadContext threadContext, String action) {
        if (isInternalAction(action) == false) {
            return false;
        }

        User user = threadContext.getTransient(InternalAuthenticationService.USER_KEY);
        if (user == null || SystemUser.is(user)) {
            return true;
        }

        // we have a internal action being executed by a user that is not the system user, lets verify that there is a
        // originating action that is not a internal action
        final String originatingAction = threadContext.getTransient(InternalAuthorizationService.ORIGINATING_ACTION_KEY);
        if (originatingAction != null && isInternalAction(originatingAction) == false) {
            return true;
        }

        // either there was no originating action or it was a internal action, we should not replace under these circumstances
        return false;
    }

    public static boolean isInternalAction(String action) {
        return INTERNAL_PREDICATE.test(action);
    }
}
