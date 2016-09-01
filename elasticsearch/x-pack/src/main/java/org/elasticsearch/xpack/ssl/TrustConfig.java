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

import javax.net.ssl.X509ExtendedTrustManager;
import java.nio.file.Path;
import java.util.List;

/**
 * The configuration of trust material for SSL usage
 */
abstract class TrustConfig {

    /**
     * Creates a {@link X509ExtendedTrustManager} based on the provided configuration
     * @param environment the environment to resolve files against or null in the case of running in a transport client
     */
    abstract X509ExtendedTrustManager createTrustManager(@Nullable Environment environment);

    /**
     * Returns a list of files that should be monitored for changes
     * @param environment the environment to resolve files against or null in the case of running in a transport client
     */
    abstract List<Path> filesToMonitor(@Nullable Environment environment);

    /**
     * {@inheritDoc}. Declared as abstract to force implementors to provide a custom implementation
     */
    public abstract String toString();

    /**
     * {@inheritDoc}. Declared as abstract to force implementors to provide a custom implementation
     */
    public abstract boolean equals(Object o);

    /**
     * {@inheritDoc}. Declared as abstract to force implementors to provide a custom implementation
     */
    public abstract int hashCode();
}
