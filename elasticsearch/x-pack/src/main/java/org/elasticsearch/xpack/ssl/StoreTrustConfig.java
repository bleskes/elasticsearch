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
import org.elasticsearch.env.Environment;

import javax.net.ssl.X509ExtendedTrustManager;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Trust configuration that is backed by a {@link java.security.KeyStore}
 */
class StoreTrustConfig extends TrustConfig {

    final String trustStorePath;
    final String trustStorePassword;
    final String trustStoreAlgorithm;

    /**
     * Create a new configuration based on the provided parameters
     * @param trustStorePath the path to the truststore
     * @param trustStorePassword the password for the truststore
     * @param trustStoreAlgorithm the algorithm to use for reading the truststore
     */
    StoreTrustConfig(String trustStorePath, String trustStorePassword, String trustStoreAlgorithm) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePath != null ?
                Objects.requireNonNull(trustStorePassword, "truststore password must be specified") : trustStorePassword;
        this.trustStoreAlgorithm = trustStoreAlgorithm;
    }

    @Override
    X509ExtendedTrustManager createTrustManager(@Nullable Environment environment) {
        try {
            return CertUtils.trustManager(trustStorePath, trustStorePassword, trustStoreAlgorithm, environment);
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a TrustManagerFactory", e);
        }
    }

    @Override
    List<Path> filesToMonitor(@Nullable Environment environment) {
        if (trustStorePath == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(CertUtils.resolvePath(trustStorePath, environment));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoreTrustConfig that = (StoreTrustConfig) o;

        if (trustStorePath != null ? !trustStorePath.equals(that.trustStorePath) : that.trustStorePath != null) return false;
        if (trustStorePassword != null ? !trustStorePassword.equals(that.trustStorePassword) : that.trustStorePassword != null)
            return false;
        return trustStoreAlgorithm != null ? trustStoreAlgorithm.equals(that.trustStoreAlgorithm) : that.trustStoreAlgorithm == null;
    }

    @Override
    public int hashCode() {
        int result = trustStorePath != null ? trustStorePath.hashCode() : 0;
        result = 31 * result + (trustStorePassword != null ? trustStorePassword.hashCode() : 0);
        result = 31 * result + (trustStoreAlgorithm != null ? trustStoreAlgorithm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "trustStorePath=[" + trustStorePath +
                "], trustStoreAlgorithm=[" + trustStoreAlgorithm +
                "]";
    }
}
