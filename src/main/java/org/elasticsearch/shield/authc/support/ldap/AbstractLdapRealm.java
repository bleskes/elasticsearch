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
import org.elasticsearch.shield.ShieldException;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.support.CachingUsernamePasswordRealm;
import org.elasticsearch.shield.authc.support.RefreshListener;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.transport.TransportMessage;

import java.util.List;
import java.util.Set;

/**
 * Supporting class for JNDI-based Realms
 */
public abstract class AbstractLdapRealm extends CachingUsernamePasswordRealm implements Realm<UsernamePasswordToken> {

    protected final LdapConnectionFactory connectionFactory;
    protected final GroupToRoleMapper roleMapper;

    public AbstractLdapRealm(Settings settings, LdapConnectionFactory ldap, GroupToRoleMapper roleMapper, RestController restController) {
        super(settings);
        this.connectionFactory = ldap;
        this.roleMapper = roleMapper;
        roleMapper.addListener(new Listener());
        restController.registerRelevantHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
    }

    @Override
    public UsernamePasswordToken token(TransportMessage<?> message) {
        return UsernamePasswordToken.extractToken(message, null);
    }

    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    /**
     * Given a username and password, connect to ldap, retrieve groups, map to roles and build the user.
     * @return User with elasticsearch roles
     */
    @Override
    protected User doAuthenticate(UsernamePasswordToken token) {
        try (LdapConnection session = connectionFactory.bind(token.principal(), token.credentials())) {
            List<String> groupDNs = session.getGroups();
            Set<String> roles = roleMapper.mapRoles(groupDNs);
            return new User.Simple(token.principal(), roles.toArray(new String[roles.size()]));
        } catch (ShieldException e){
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
}
