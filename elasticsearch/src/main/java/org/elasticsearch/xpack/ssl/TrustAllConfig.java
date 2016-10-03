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

package org.elasticsearch.xpack.ssl;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.env.Environment;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * A trust manager that trusts all certificates
 */
class TrustAllConfig extends TrustConfig {

    public static final TrustAllConfig INSTANCE = new TrustAllConfig();

    /**
     * The {@link X509ExtendedTrustManager} that will trust all certificates. All methods are implemented as a no-op and do not throw
     * exceptions regardless of the certificate presented.
     */
    private static final X509ExtendedTrustManager TRUST_MANAGER = new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private TrustAllConfig() {
    }

    @Override
    X509ExtendedTrustManager createTrustManager(@Nullable Environment environment) {
        return TRUST_MANAGER;
    }

    @Override
    List<Path> filesToMonitor(@Nullable Environment environment) {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "trust all";
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
