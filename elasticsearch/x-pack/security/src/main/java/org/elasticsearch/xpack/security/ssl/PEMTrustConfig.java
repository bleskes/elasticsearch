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

import javax.net.ssl.X509ExtendedTrustManager;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

class PEMTrustConfig extends TrustConfig {

    final List<String> caPaths;

    PEMTrustConfig(boolean includeSystem, List<String> caPaths) {
        super(includeSystem);
        this.caPaths = caPaths;
    }

    @Override
    X509ExtendedTrustManager nonSystemTrustManager(@Nullable Environment environment) {
        try {
            Certificate[] certificates = CertUtils.readCertificates(caPaths, environment);
            return CertUtils.trustManagers(certificates);
        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize a TrustManagerFactory", e);
        }
    }

    @Override
    void validate() {
        if (caPaths == null) {
            throw new IllegalArgumentException("no ca paths have been configured");
        }
    }

    @Override
    List<Path> filesToMonitor(@Nullable Environment environment) {
        List<Path> paths = new ArrayList<>(caPaths.size());
        for (String path : caPaths) {
            paths.add(CertUtils.resolvePath(path, environment));
        }
        return paths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PEMTrustConfig that = (PEMTrustConfig) o;

        return caPaths != null ? caPaths.equals(that.caPaths) : that.caPaths == null;

    }

    @Override
    public int hashCode() {
        return caPaths != null ? caPaths.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ca=[" + Strings.collectionToCommaDelimitedString(caPaths) + "]";
    }
}
