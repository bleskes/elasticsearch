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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.Closeable;
import java.util.List;

/**
 * Represents a LDAP connection with an authenticated/bound user that needs closing.
 */
public abstract class AbstractLdapConnection implements Closeable {

    protected final ESLogger logger;
    protected final DirContext jndiContext;
    protected final String bindDn;
    protected final GroupsResolver groupsResolver;
    protected final TimeValue timeout;

    /**
     * This object is intended to be constructed by the LdapConnectionFactory
     *
     * This constructor accepts a logger with wich the connection can log. Since this connection
     * can be instantiated very frequently, it's best to have the logger for this connection created
     * outside of and be reused across all connections. We can't keep a static logger in this class
     * since we want the logger to be contextual (i.e. aware of the settings and its enviorment).
     */
    public AbstractLdapConnection(ESLogger logger, DirContext ctx, String boundName, GroupsResolver groupsResolver, TimeValue timeout) {
        this.logger = logger;
        this.jndiContext = ctx;
        this.bindDn = boundName;
        this.groupsResolver = groupsResolver;
        this.timeout = timeout;
    }

    /**
     * LDAP connections should be closed to clean up resources.  However, the jndi contexts have the finalize
     * implemented properly so that it will clean up on garbage collection.
     */
    @Override
    public void close(){
        try {
            jndiContext.close();
        } catch (NamingException e) {
            throw new SecurityException("could not close the LDAP connection", e);
        }
    }

    /**
     * @return the fully distinguished name of the user bound to this connection
     */
    public String authenticatedUserDn() {
        return bindDn;
    }

    /**
     * @return List of fully distinguished group names
     */
    public List<String> groups() {
        return groupsResolver.resolve(jndiContext, bindDn, timeout);
    }

    public static interface GroupsResolver {

        List<String> resolve(DirContext ctx, String userDn, TimeValue timeout);

    }
}
