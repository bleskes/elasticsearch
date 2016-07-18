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

package org.elasticsearch.example.realm;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.authc.AuthenticationToken;
import org.elasticsearch.xpack.security.authc.Realm;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;

public class CustomRealm extends Realm {

    public static final String TYPE = "custom";

    public static final String USER_HEADER = "User";
    public static final String PW_HEADER = "Password";

    static final String KNOWN_USER = "custom_user";
    static final String KNOWN_PW = "changeme";
    static final String[] ROLES = new String[] { "superuser" };

    public CustomRealm(RealmConfig config) {
        super(TYPE, config);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    @Override
    public UsernamePasswordToken token(ThreadContext threadContext) {
        String user = threadContext.getHeader(USER_HEADER);
        if (user != null) {
            String password = threadContext.getHeader(PW_HEADER);
            if (password != null) {
                return new UsernamePasswordToken(user, new SecuredString(password.toCharArray()));
            }
        }
        return null;
    }

    @Override
    public User authenticate(AuthenticationToken authToken) {
        UsernamePasswordToken token = (UsernamePasswordToken)authToken;
        final String actualUser = token.principal();
        if (KNOWN_USER.equals(actualUser) && SecuredString.constantTimeEquals(token.credentials(), KNOWN_PW)) {
            return new User(actualUser, ROLES);
        }
        return null;
    }

    @Override
    public User lookupUser(String username) {
        return null;
    }

    @Override
    public boolean userLookupSupported() {
        return false;
    }
}
