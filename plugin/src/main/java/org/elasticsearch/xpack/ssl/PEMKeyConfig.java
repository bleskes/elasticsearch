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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.env.Environment;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of a key configuration that is backed by a PEM encoded key file and one or more certificates
 */
class PEMKeyConfig extends KeyConfig {

    private final String keyPath;
    private final String keyPassword;
    private final String certPath;

    /**
     * Creates a new key configuration backed by the key and certificate chain provided
     * @param keyPath the path to the key file
     * @param keyPassword the password for the key. May be {@code null}
     * @param certChainPath the path to the file containing the certificate chain
     */
    PEMKeyConfig(String keyPath, String keyPassword, String certChainPath) {
        this.keyPath = Objects.requireNonNull(keyPath, "key file must be specified");
        this.keyPassword = keyPassword;
        this.certPath = Objects.requireNonNull(certChainPath, "certificate must be specified");
    }

    @Override
    X509ExtendedKeyManager createKeyManager(@Nullable Environment environment) {
        // password must be non-null for keystore...
        try {
            PrivateKey privateKey = readPrivateKey(CertUtils.resolvePath(keyPath, environment));
            Certificate[] certificateChain = CertUtils.readCertificates(Collections.singletonList(certPath), environment);
            // password must be non-null for keystore...
            try (SecureString securedKeyPasswordChars = new SecureString(keyPassword == null ? new char[0] : keyPassword.toCharArray())) {
                return CertUtils.keyManager(certificateChain, privateKey, securedKeyPasswordChars.getChars());
            }
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a KeyManagerFactory", e);
        }
    }

    private PrivateKey readPrivateKey(Path keyPath) throws Exception {
        try (Reader reader = Files.newBufferedReader(keyPath, StandardCharsets.UTF_8);
             SecureString secureString = new SecureString(keyPassword == null ? new char[0] : keyPassword.toCharArray())) {
            return CertUtils.readPrivateKey(reader, () -> {
                if (keyPassword == null) {
                    return null;
                } else {
                    return secureString.getChars();
                }
            });
        }
    }

    @Override
    X509ExtendedTrustManager createTrustManager(@Nullable Environment environment) {
        try {
            Certificate[] certificates = CertUtils.readCertificates(Collections.singletonList(certPath), environment);
            return CertUtils.trustManager(certificates);
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a TrustManagerFactory", e);
        }
    }

    @Override
    List<Path> filesToMonitor(@Nullable Environment environment) {
        List<Path> paths = new ArrayList<>(2);
        paths.add(CertUtils.resolvePath(keyPath, environment));
        paths.add(CertUtils.resolvePath(certPath, environment));
        return paths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PEMKeyConfig that = (PEMKeyConfig) o;

        if (keyPath != null ? !keyPath.equals(that.keyPath) : that.keyPath != null) return false;
        if (keyPassword != null ? !keyPassword.equals(that.keyPassword) : that.keyPassword != null) return false;
        return certPath != null ? certPath.equals(that.certPath) : that.certPath == null;

    }

    @Override
    public int hashCode() {
        int result = keyPath != null ? keyPath.hashCode() : 0;
        result = 31 * result + (keyPassword != null ? keyPassword.hashCode() : 0);
        result = 31 * result + (certPath != null ? certPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "keyPath=[" + keyPath +
                "], certPaths=[" + certPath +
                "]";
    }
}
