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

package org.elasticsearch.shield.authc.ldap.support;

import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.*;

import java.util.List;
import java.util.Set;

/**
 * Supporting class for JNDI-based Realms
 */
public abstract class AbstractLdapRealm extends CachingUsernamePasswordRealm {

    protected final SessionFactory sessionFactory;
    protected final DnRoleMapper roleMapper;

    protected AbstractLdapRealm(String type, RealmConfig config,
                                SessionFactory sessionFactory, DnRoleMapper roleMapper) {
        super(type, config);
        this.sessionFactory = sessionFactory;
        this.roleMapper = roleMapper;
        roleMapper.addListener(new Listener());
    }

    /**
     * Given a username and password, open to ldap, retrieve groups, map to roles and build the user.
     *
     * @return User with elasticsearch roles
     */
    @Override
    protected User doAuthenticate(UsernamePasswordToken token) {
        try (LdapSession session = sessionFactory.session(token.principal(), token.credentials())) {
            List<String> groupDNs = session.groups();
            Set<String> roles = roleMapper.resolveRoles(session.userDn(), groupDNs);
            return new User.Simple(token.principal(), roles.toArray(new String[roles.size()]));
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                logger.debug("authentication failed for user [{}]", e, token.principal());
            } else {
                String causeMessage = (e.getCause() == null) ? null : e.getCause().getMessage();
                if (causeMessage == null) {
                    logger.warn("authentication failed for user [{}]: {}", token.principal(), e.getMessage());
                } else {
                    logger.warn("authentication failed for user [{}]: {}\ncause: {}: {}", token.principal(), e.getMessage(), e.getCause().getClass().getName(), causeMessage);
                }
            }
            return null;
        }
    }

    class Listener implements RefreshListener {
        @Override
        public void onRefresh() {
            expireAll();
        }
    }

    public static abstract class Factory<R extends AbstractLdapRealm> extends UsernamePasswordRealm.Factory<R> {

        public Factory(String type) {
            super(type, false);
        }

        /**
         * LDAP realms require minimum settings (e.g. URL), therefore they'll never create a default.
         *
         * @return {@code null} always
         */
        @Override
        public final R createDefault(String name) {
            return null;
        }
    }
}
