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
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;

/**
 * This factory is needed for JNDI configuration for LDAP connections with hostname verification. Each SSLSocket must
 * have the appropriate SSLParameters set to indicate that hostname verification is required
 */
public class HostnameVerifyingLdapSslSocketFactory extends AbstractLdapSslSocketFactory {

    private static final ESLogger logger = Loggers.getLogger(HostnameVerifyingLdapSslSocketFactory.class);

    private static HostnameVerifyingLdapSslSocketFactory instance;
    private final SSLParameters sslParameters;

    public HostnameVerifyingLdapSslSocketFactory(SSLSocketFactory socketFactory) {
        super(socketFactory);
        sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("LDAPS");
    }

    /**
     * This is invoked by JNDI and the returned SocketFactory must be an HostnameVerifyingLdapSslSocketFactory object
     *
     * @return a singleton instance of HostnameVerifyingLdapSslSocketFactory set by calling the init static method.
     */
    public static synchronized SocketFactory getDefault() {
        if (instance == null) {
            instance = new HostnameVerifyingLdapSslSocketFactory(sslService.getSSLSocketFactory());
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

    /**
     * Configures the socket to require hostname verification using the LDAPS
     * @param sslSocket
     */
    @Override
    protected void configureSSLSocket(SSLSocket sslSocket) {
        sslSocket.setSSLParameters(sslParameters);
    }
}
