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

package org.elasticsearch.xpack.upgrade;

import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.xpack.security.InternalClient;

import java.util.Collection;
import java.util.Collections;

/**
 * Factory for index checks
 */
public interface IndexUpgradeCheckFactory {

    /**
     * Using this method the check can expose additional user parameter that can be specified by the user on upgrade api
     *
     * @return the list of supported parameters
     */
    default Collection<String> supportedParams() {
        return Collections.emptyList();
    }

    /**
     * Creates an upgrade check
     * <p>
     * This method is called from {@link org.elasticsearch.plugins.Plugin#createComponents} method.
     */
    IndexUpgradeCheck createCheck(InternalClient internalClient, ClusterService clusterService);

}
