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

package org.elasticsearch.shield.authc.support.ldap;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.shield.authc.support.RefreshListener;
import org.elasticsearch.shield.authc.support.UsernamePasswordRealm;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;

import java.util.List;
import java.util.Set;

/**
 * Supporting class for JNDI-based Realms
 */
public abstract class AbstractLdapRealm extends CachingUsernamePasswordRealm {

    protected final ConnectionFactory connectionFactory;
    protected final AbstractGroupToRoleMapper roleMapper;

    protected AbstractLdapRealm(String type, String name, Settings settings,
                                ConnectionFactory connectionFactory, AbstractGroupToRoleMapper roleMapper) {
        super(type, name, settings);
        this.connectionFactory = connectionFactory;
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
        try (AbstractLdapConnection session = connectionFactory.open(token.principal(), token.credentials())) {
            List<String> groupDNs = session.groups();
            Set<String> roles = roleMapper.mapRoles(groupDNs);
            return new User.Simple(token.principal(), roles.toArray(new String[roles.size()]));
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication Failed for user [{}]", e, token.principal());
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

        public Factory(String type, RestController restController) {
            super(type, false, restController);
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
