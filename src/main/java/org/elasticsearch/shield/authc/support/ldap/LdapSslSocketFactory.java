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
import org.elasticsearch.common.logging.Loggers;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * This factory is needed for JNDI configuration for LDAP connections.  It wraps a single instance of a static
 * factory that is initiated by the settings constructor.  JNDI uses reflection to call the getDefault() static method
 * then checks to make sure that the factory returned is an LdapSslSocketFactory.  Because of this we have to wrap
 * the socket factory
 * <p/>
 * http://docs.oracle.com/javase/tutorial/jndi/ldap/ssl.html
 */
public class LdapSslSocketFactory extends AbstractLdapSslSocketFactory {

    private static final ESLogger logger = Loggers.getLogger(LdapSslSocketFactory.class);

    private static LdapSslSocketFactory instance;

    public LdapSslSocketFactory(SSLSocketFactory socketFactory) {
        super(socketFactory);
    }

    /**
     * This is invoked by JNDI and the returned SocketFactory must be an LdapSslSocketFactory object
     *
     * @return a singleton instance of LdapSslSocketFactory set by calling the init static method.
     */
    public static synchronized SocketFactory getDefault() {
        if (instance == null) {
            instance = new LdapSslSocketFactory(sslService.getSSLSocketFactory());
        }
        return instance;
    }

    /**
     * This clears the static factory.  There are threading issues with this.  But for
     * testing this is useful.
     *
     * WARNING: THIS METHOD SHOULD ONLY BE CALLED IN TESTS!!!!
     *
     * TODO: find a way to change the tests such that we can remove this method
     */
    public static void clear() {
        logger.error("clear should only be called by tests");
        instance = null;
    }
}
