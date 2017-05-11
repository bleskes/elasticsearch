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

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import java.util.Map;

/**
 * Generic upgrade check applicable to all indices to be upgraded from the current version
 * to the next major version
 */
public class GenericIndexUpgradeCheck implements IndexUpgradeCheck {
    @Override
    public String getName() {
        return "generic";
    }

    @Override
    public UpgradeActionRequired actionRequired(IndexMetaData indexMetaData, Map<String, String> params, ClusterState state) {
        if (indexMetaData.getCreationVersion().before(Version.V_5_0_0_alpha1)) {
            return UpgradeActionRequired.REINDEX;
        }
        return UpgradeActionRequired.UP_TO_DATE;
    }
}
