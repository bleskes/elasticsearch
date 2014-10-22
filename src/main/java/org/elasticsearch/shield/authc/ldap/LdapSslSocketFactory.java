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

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldSettingsException;
import org.elasticsearch.shield.transport.ssl.SSLTrustConfig;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Locale;

/**
 * This factory is needed for JNDI configuration for LDAP connections.  It wraps a single instance of a static
 * factory that is initiated by the settings constructor.  JNDI uses reflection to call the getDefault() static method
 * then checks to make sure that the factory returned is an LdapSslSocketFactory.  Because of this we have to wrap
 * the socket factory
 *
 * http://docs.oracle.com/javase/tutorial/jndi/ldap/ssl.html
 */
public class LdapSslSocketFactory extends SocketFactory {

    static final String JAVA_NAMING_LDAP_FACTORY_SOCKET = "java.naming.ldap.factory.socket";
    private static ESLogger logger = ESLoggerFactory.getLogger(LdapSslSocketFactory.class.getName());
    private static LdapSslSocketFactory instance;

    /**
     * This should only be invoked once to establish a static instance that will be used for each constructor.
     */
    public static void init(Settings settings) {
        if (instance != null) {
            logger.error("LdapSslSocketFactory already configured, this change could lead to threading issues");
        }

        Settings componentSettings = settings.getComponentSettings(LdapSslSocketFactory.class);
        Settings generalSslSettings = settings.getByPrefix("shield.ssl.");
        if (generalSslSettings.get("truststore") == null && componentSettings.get("truststore") == null){
            logger.warn("No truststore has been configured for LDAP");
        } else {
            SSLTrustConfig sslConfig = new SSLTrustConfig(componentSettings, generalSslSettings);
            instance = new LdapSslSocketFactory(sslConfig.createSSLSocketFactory());
        }
    }

    /**
     * This is invoked by JNDI and the returned SocketFactory must be an LdapSslSocketFactory object
     * @return a singleton instance of LdapSslSocketFactory set by calling the init static method.
     */
    public static SocketFactory getDefault() {
        assert instance != null;
        return instance;
    }

    public static boolean initialized() {
        return instance != null;
    }

    final private SocketFactory socketFactory;

    private LdapSslSocketFactory(SocketFactory wrappedSocketFactory){
        socketFactory = wrappedSocketFactory;
    }

    //The following methods are all wrappers around the instance of socketFactory

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        return socketFactory.createSocket(s, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException {
        return socketFactory.createSocket(s, i, inetAddress, i2);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return socketFactory.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        return socketFactory.createSocket(inetAddress, i, inetAddress2, i2);
    }

    /**
     * If one of the ldapUrls are SSL this will set the LdapSslSocketFactory as a socket provider on the builder
     * @param ldapUrls array of ldap urls, either all SSL or none with SSL (no mixing)
     * @param builder set of jndi properties, that will
     * @throws org.elasticsearch.shield.ShieldSettingsException if URLs have mixed protocols.
     */
    public static void configureJndiSSL(String[] ldapUrls, ImmutableMap.Builder<String, Serializable> builder) {
        boolean secureProtocol = ldapUrls[0].toLowerCase(Locale.getDefault()).startsWith("ldaps://");
        for(String url: ldapUrls){
            if (secureProtocol != url.toLowerCase(Locale.getDefault()).startsWith("ldaps://")) {
                //this is because LdapSSLSocketFactory produces only SSL sockets and not clear text sockets
                throw new ShieldSettingsException("Configured ldap protocols are not all equal " +
                        "(ldaps://.. and ldap://..): [" + Strings.arrayToCommaDelimitedString(ldapUrls) + "]");
            }
        }
        if (secureProtocol && instance != null) {
            builder.put(JAVA_NAMING_LDAP_FACTORY_SOCKET, LdapSslSocketFactory.class.getName());
        } else {
            logger.warn("LdapSslSocketFactory not used for LDAP connections");
        }
    }
}
