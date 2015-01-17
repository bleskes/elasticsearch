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

package org.elasticsearch.shield.authc.ldap;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapConnection;

import javax.naming.directory.DirContext;

/**
 * Encapsulates jndi/ldap functionality into one authenticated connection.  The constructor is package scoped, assuming
 * instances of this connection will be produced by the LdapConnectionFactory.open() methods.
 * <p/>
 * A standard looking usage pattern could look like this:
 * <pre>
 * try (LdapConnection session = ldapFac.bindXXX(...);
 * ...do stuff with the session
 * }
 * </pre>
 */
public class LdapConnection extends AbstractLdapConnection {

    /**
     * This object is intended to be constructed by the LdapConnectionFactory
     */
    LdapConnection(ESLogger logger, DirContext ctx, String bindDN, AbstractLdapConnection.GroupsResolver groupsResolver, TimeValue timeout) {
        super(logger, ctx, bindDN, groupsResolver, timeout);
    }
}
