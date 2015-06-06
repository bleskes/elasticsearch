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

package org.elasticsearch.shield.rest;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.netty.NettyHttpRequest;
import org.elasticsearch.rest.*;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authc.pki.PkiRealm;
import org.elasticsearch.shield.transport.SSLClientAuth;
import org.elasticsearch.shield.transport.netty.ShieldNettyHttpServerTransport;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 *
 */
public class ShieldRestFilter extends RestFilter {

    private final AuthenticationService service;
    private final ESLogger logger;
    private final boolean extractClientCertificate;

    @Inject
    public ShieldRestFilter(AuthenticationService service, RestController controller, Settings settings) {
        this.service = service;
        controller.registerFilter(this);
        boolean ssl = settings.getAsBoolean(ShieldNettyHttpServerTransport.HTTP_SSL_SETTING, ShieldNettyHttpServerTransport.HTTP_SSL_DEFAULT);
        extractClientCertificate = ssl && SSLClientAuth.parse(settings.get(ShieldNettyHttpServerTransport.HTTP_CLIENT_AUTH_SETTING), ShieldNettyHttpServerTransport.HTTP_CLIENT_AUTH_DEFAULT).enabled();
        logger = Loggers.getLogger(getClass(), settings);
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void process(RestRequest request, RestChannel channel, RestFilterChain filterChain) throws Exception {

        // CORS - allow for preflight unauthenticated OPTIONS request
        if (request.method() != RestRequest.Method.OPTIONS) {
            if (extractClientCertificate) {
                putClientCertificateInContext(request, logger);
            }
            service.authenticate(request);
        }

        RemoteHostHeader.process(request);

        filterChain.continueProcessing(request, channel);
    }

    static void putClientCertificateInContext(RestRequest request, ESLogger logger) throws Exception {
        assert request instanceof NettyHttpRequest;
        NettyHttpRequest nettyHttpRequest = (NettyHttpRequest) request;

        SslHandler handler = nettyHttpRequest.getChannel().getPipeline().get(SslHandler.class);
        assert handler != null;
        try {
            Certificate[] certs = handler.getEngine().getSession().getPeerCertificates();
            if (certs instanceof X509Certificate[]) {
                request.putInContext(PkiRealm.PKI_CERT_HEADER_NAME, certs);
            }
        } catch (SSLPeerUnverifiedException e) {
            // this happens when we only request client authentication and the client does not provide it
            if (logger.isTraceEnabled()) {
                logger.trace("SSL Peer did not present a certificate on channel [{}]", e, nettyHttpRequest.getChannel());
            } else if (logger.isDebugEnabled()) {
                logger.debug("SSL Peer did not present a certificate on channel [{}]", nettyHttpRequest.getChannel());
            }
        }
    }
}
