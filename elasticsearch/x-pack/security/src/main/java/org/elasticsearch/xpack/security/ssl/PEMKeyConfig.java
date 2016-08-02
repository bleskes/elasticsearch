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

package org.elasticsearch.xpack.security.ssl;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
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
import java.util.Arrays;
import java.util.List;

class PEMKeyConfig extends KeyConfig {

    final String keyPath;
    final String keyPassword;
    final List<String> certPaths;

    PEMKeyConfig(boolean includeSystem, String keyPath, String keyPassword, List<String> certPaths) {
        super(includeSystem);
        this.keyPath = keyPath;
        this.keyPassword = keyPassword;
        this.certPaths = certPaths;
    }

    @Override
    X509ExtendedKeyManager loadKeyManager(@Nullable Environment environment) {
        // password must be non-null for keystore...
        char[] password = keyPassword == null ? new char[0] : keyPassword.toCharArray();
        try {
            PrivateKey privateKey = readPrivateKey(CertUtils.resolvePath(keyPath, environment));
            Certificate[] certificateChain = CertUtils.readCertificates(certPaths, environment);
            return CertUtils.keyManagers(certificateChain, privateKey, password);
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a KeyManagerFactory", e);
        } finally {
            if (password != null) {
                Arrays.fill(password, (char) 0);
            }
        }
    }

    PrivateKey readPrivateKey(Path keyPath) throws Exception {
        char[] password = keyPassword == null ? null : keyPassword.toCharArray();
        try (Reader reader = Files.newBufferedReader(keyPath, StandardCharsets.UTF_8)) {
            return CertUtils.readPrivateKey(reader, () -> password);
        } finally {
            if (password != null) {
                Arrays.fill(password, (char) 0);
            }
        }
    }

    @Override
    X509ExtendedTrustManager nonSystemTrustManager(@Nullable Environment environment) {
        try {
            Certificate[] certificates = CertUtils.readCertificates(certPaths, environment);
            return CertUtils.trustManagers(certificates);
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a TrustManagerFactory", e);
        }
    }

    @Override
    void validate() {
        if (keyPath == null) {
            throw new IllegalArgumentException("no key file configured");
        } else if (certPaths == null || certPaths.isEmpty()) {
            throw new IllegalArgumentException("no certificate provided");
        }
    }

    @Override
    List<Path> filesToMonitor(@Nullable Environment environment) {
        List<Path> paths = new ArrayList<>(1 + certPaths.size());
        paths.add(CertUtils.resolvePath(keyPath, environment));
        for (String certPath : certPaths) {
            paths.add(CertUtils.resolvePath(certPath, environment));
        }
        return paths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PEMKeyConfig that = (PEMKeyConfig) o;

        if (keyPath != null ? !keyPath.equals(that.keyPath) : that.keyPath != null) return false;
        if (keyPassword != null ? !keyPassword.equals(that.keyPassword) : that.keyPassword != null) return false;
        return certPaths != null ? certPaths.equals(that.certPaths) : that.certPaths == null;

    }

    @Override
    public int hashCode() {
        int result = keyPath != null ? keyPath.hashCode() : 0;
        result = 31 * result + (keyPassword != null ? keyPassword.hashCode() : 0);
        result = 31 * result + (certPaths != null ? certPaths.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "keyPath=[" + keyPath +
                "], certPaths=[" + Strings.collectionToCommaDelimitedString(certPaths) +
                "]";
    }
}
