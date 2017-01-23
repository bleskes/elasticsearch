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

package org.elasticsearch.xpack.security.rest;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.http.netty3.Netty3HttpRequest;
import org.elasticsearch.http.netty4.Netty4HttpRequest;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestRequest.Method;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.authc.pki.PkiRealm;
import org.elasticsearch.xpack.ssl.SSLService;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.io.IOException;

import static org.elasticsearch.xpack.XPackSettings.HTTP_SSL_ENABLED;

public class SecurityRestFilter implements RestHandler {

    private static final Logger logger = ESLoggerFactory.getLogger(SecurityRestFilter.class);

    private final RestHandler restHandler;
    private final AuthenticationService service;
    private final XPackLicenseState licenseState;
    private final ThreadContext threadContext;
    private final boolean extractClientCertificate;

    public SecurityRestFilter(Settings settings, XPackLicenseState licenseState, SSLService sslService,
                              ThreadContext threadContext, AuthenticationService service, RestHandler restHandler) {
        this.restHandler = restHandler;
        this.service = service;
        this.licenseState = licenseState;
        this.threadContext = threadContext;
        final boolean ssl = HTTP_SSL_ENABLED.get(settings);
        Settings httpSSLSettings = SSLService.getHttpTransportSSLSettings(settings);
        this.extractClientCertificate = ssl && sslService.isSSLClientAuthEnabled(httpSSLSettings);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        if (licenseState.isAuthAllowed() && request.method() != Method.OPTIONS) {
            // CORS - allow for preflight unauthenticated OPTIONS request
            if (extractClientCertificate) {
                putClientCertificateInContext(request, threadContext, logger);
            }
            service.authenticate(maybeWrapRestRequest(request), ActionListener.wrap(
                authentication -> {
                    RemoteHostHeader.process(request, threadContext);
                    restHandler.handleRequest(request, channel, client);
                }, e -> {
                    try {
                        channel.sendResponse(new BytesRestResponse(channel, e));
                    } catch (Exception inner) {
                        inner.addSuppressed(e);
                        logger.error((Supplier<?>) () ->
                            new ParameterizedMessage("failed to send failure response for uri [{}]", request.uri()), inner);
                    }
            }));
        } else {
            restHandler.handleRequest(request, channel, client);
        }
    }

    private static void putClientCertificateInContext(RestRequest request, ThreadContext threadContext, Logger logger) throws Exception {
        assert request instanceof Netty3HttpRequest || request instanceof Netty4HttpRequest;
        if (request instanceof Netty3HttpRequest) {
            Netty3HttpRequest nettyHttpRequest = (Netty3HttpRequest) request;

            SslHandler handler = nettyHttpRequest.getChannel().getPipeline().get(SslHandler.class);
            assert handler != null;
            extractClientCerts(handler.getEngine(), nettyHttpRequest.getChannel(), threadContext, logger);
        } else if (request instanceof Netty4HttpRequest) {
            Netty4HttpRequest nettyHttpRequest = (Netty4HttpRequest) request;

            io.netty.handler.ssl.SslHandler handler = nettyHttpRequest.getChannel().pipeline().get(io.netty.handler.ssl.SslHandler.class);
            assert handler != null;
            extractClientCerts(handler.engine(), nettyHttpRequest.getChannel(), threadContext, logger);
        }

    }

    private static void extractClientCerts(SSLEngine sslEngine, Object channel, ThreadContext threadContext, Logger logger) {
        try {
            Certificate[] certs = sslEngine.getSession().getPeerCertificates();
            if (certs instanceof X509Certificate[]) {
                threadContext.putTransient(PkiRealm.PKI_CERT_HEADER_NAME, certs);
            }
        } catch (SSLPeerUnverifiedException e) {
            // this happens when client authentication is optional and the client does not provide credentials. If client
            // authentication was required then this connection should be closed before ever getting into this class
            assert sslEngine.getNeedClientAuth() == false;
            assert sslEngine.getWantClientAuth();
            if (logger.isTraceEnabled()) {
                logger.trace(
                        (Supplier<?>) () -> new ParameterizedMessage("SSL Peer did not present a certificate on channel [{}]", channel), e);
            } else if (logger.isDebugEnabled()) {
                logger.debug("SSL Peer did not present a certificate on channel [{}]", channel);
            }
        }
    }

    RestRequest maybeWrapRestRequest(RestRequest restRequest) throws IOException {
        if (restHandler instanceof RestRequestFilter) {
            return ((RestRequestFilter)restHandler).getFilteredRequest(restRequest);
        }
        return restRequest;
    }
}
