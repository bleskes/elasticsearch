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

import org.elasticsearch.shield.authc.ldap.LdapException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.io.Closeable;

/**
 * ClosableNamingEnumeration wraps a NamingEnumeration so it can be used in a try with resources block and auto-closed.
 */
public class ClosableNamingEnumeration<T> implements Closeable, NamingEnumeration<T> {
    private final NamingEnumeration<T> namingEnumeration;

    public ClosableNamingEnumeration(NamingEnumeration<T> namingEnumeration) {
        this.namingEnumeration = namingEnumeration;
    }

    @Override
    public T next() throws NamingException {
        return namingEnumeration.next();
    }

    @Override
    public boolean hasMore() throws NamingException {
        return namingEnumeration.hasMore();
    }

    @Override
    public void close() {
        try {
            namingEnumeration.close();
        } catch (NamingException e) {
            throw new LdapException("Error occurred trying to close a naming enumeration", e);
        }
    }

    @Override
    public boolean hasMoreElements() {
        return namingEnumeration.hasMoreElements();
    }

    @Override
    public T nextElement() {
        return namingEnumeration.nextElement();
    }
}
