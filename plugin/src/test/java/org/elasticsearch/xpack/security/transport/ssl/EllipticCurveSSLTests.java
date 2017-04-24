/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.transport.ssl;

import com.unboundid.util.ssl.TrustAllTrustManager;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.xpack.ssl.CertUtils;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;

public class EllipticCurveSSLTests extends SecurityIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        final Path keyPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-key.pem");
        final Path certPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-cert.pem");
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal).filter(s -> s.startsWith("xpack.ssl") == false))
                .put("xpack.ssl.key", keyPath)
                .put("xpack.ssl.certificate", certPath)
                .put("xpack.ssl.certificate_authorities", certPath)
                .put("xpack.ssl.verification_mode", "certificate") // disable hostname verificate since these certs aren't setup for that
                .build();
    }

    @Override
    protected Settings transportClientSettings() {
        final Path keyPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-key.pem");
        final Path certPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-cert.pem");
        return Settings.builder()
                .put(super.transportClientSettings().filter(s -> s.startsWith("xpack.ssl") == false))
                .put("xpack.ssl.key", keyPath)
                .put("xpack.ssl.certificate", certPath)
                .put("xpack.ssl.certificate_authorities", certPath)
                .put("xpack.ssl.verification_mode", "certificate") // disable hostname verification since these certs aren't setup for that
                .build();
    }

    @Override
    public boolean sslTransportEnabled() {
        return true;
    }

    public void testConnection() throws Exception {
        final Path keyPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-key.pem");
        final Path certPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/prime256v1-cert.pem");
        PrivateKey privateKey;
        try (Reader reader = Files.newBufferedReader(keyPath)) {
            privateKey = CertUtils.readPrivateKey(reader, () -> null);
        }
        Certificate[] certs = CertUtils.readCertificates(Collections.singletonList(certPath.toString()), null);
        X509ExtendedKeyManager x509ExtendedKeyManager = CertUtils.keyManager(certs, privateKey, new char[0]);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new X509ExtendedKeyManager[] { x509ExtendedKeyManager },
                new TrustManager[] { new TrustAllTrustManager(false) }, new SecureRandom());
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().setTransport(true).get();
        InetSocketTransportAddress address =
                (InetSocketTransportAddress) randomFrom(response.getNodes()).getTransport().getAddress().publishAddress();

        final CountDownLatch latch = new CountDownLatch(1);
        try (SSLSocket sslSocket = AccessController.doPrivileged(new PrivilegedExceptionAction<SSLSocket>() {
              @Override
              public SSLSocket run() throws Exception {
                  return (SSLSocket) socketFactory.createSocket(address.address().getAddress(), address.address().getPort());
              }})) {
            final AtomicReference<HandshakeCompletedEvent> reference = new AtomicReference<>();
            sslSocket.addHandshakeCompletedListener((event) -> {
                reference.set(event);
                latch.countDown();
            });
            sslSocket.startHandshake();
            latch.await();

            HandshakeCompletedEvent event = reference.get();
            assertNotNull(event);
            SSLSession session = event.getSession();
            Certificate[] peerChain = session.getPeerCertificates();
            assertEquals(1, peerChain.length);
            assertEquals(certs[0], peerChain[0]);
            assertThat(session.getCipherSuite(), containsString("ECDSA"));
        }
    }
}
