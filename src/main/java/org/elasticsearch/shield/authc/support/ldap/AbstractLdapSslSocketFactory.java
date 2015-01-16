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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.shield.ssl.SSLService;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Abstract class that wraps a SSLSocketFactory and uses it to create sockets for use with LDAP via JNDI
 */
public abstract class AbstractLdapSslSocketFactory extends SocketFactory {

    protected static SSLService sslService;

    private final SSLSocketFactory socketFactory;

    /**
     * This should only be invoked once to establish a static instance that will be used for each constructor.
     */
    @Inject
    public static void init(SSLService sslService) {
        AbstractLdapSslSocketFactory.sslService = sslService;
    }

    public AbstractLdapSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        socketFactory = sslSocketFactory;
    }

    //The following methods are all wrappers around the instance of socketFactory

    @Override
    public SSLSocket createSocket() throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket();
        configureSSLSocket(socket);
        return socket;
    }

    @Override
    public SSLSocket createSocket(String host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port);
        configureSSLSocket(socket);
        return socket;
    }

    @Override
    public SSLSocket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port, localHost, localPort);
        configureSSLSocket(socket);
        return socket;
    }

    @Override
    public SSLSocket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port);
        configureSSLSocket(socket);
        return socket;
    }

    @Override
    public SSLSocket createSocket(InetAddress host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port, localHost, localPort);
        configureSSLSocket(socket);
        return socket;
    }

    /**
     * This method allows for performing additional configuration on each socket. All 'createSocket' methods will
     * call this method before returning the socket to the caller. The default implementation is a no-op
     * @param sslSocket
     */
    protected void configureSSLSocket(SSLSocket sslSocket) {
    }
}
