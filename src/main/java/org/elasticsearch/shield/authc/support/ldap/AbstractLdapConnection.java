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

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.Closeable;
import java.util.List;

/**
 * Represents a LDAP connection with an authenticated/bound user that needs closing.
 */
public abstract class AbstractLdapConnection implements Closeable {

    protected final DirContext jndiContext;
    protected final String bindDn;

    /**
     * This object is intended to be constructed by the LdapConnectionFactory
     */
    public AbstractLdapConnection(DirContext ctx, String boundName) {
        this.jndiContext = ctx;
        this.bindDn = boundName;
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
            throw new SecurityException("Could not close the LDAP connection", e);
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
    public abstract List<String> groups();
}
