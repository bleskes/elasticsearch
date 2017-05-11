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

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.tasks.TaskResultsService;
import org.elasticsearch.xpack.security.SecurityLifecycleService;
import org.elasticsearch.xpack.watcher.watch.Watch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic upgrade check applicable to all indices to be upgraded from the current version
 * to the next major version
 */
public class InternalIndexUpgradeCheck implements IndexUpgradeCheck {
    private final Set<String> KNOWN_INTERNAL_INDICES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            Watch.INDEX,
            SecurityLifecycleService.SECURITY_INDEX_NAME,
            TaskResultsService.TASK_INDEX
    )));


    @Override
    public String getName() {
        return "inner";
    }

    @Override
    public UpgradeActionRequired actionRequired(IndexMetaData indexMetaData, Map<String, String> params, ClusterState state) {
        String indexName = indexMetaData.getIndex().getName();
        if (KNOWN_INTERNAL_INDICES.contains(indexName)) {
            return UpgradeActionRequired.UPGRADE;
        }
        if (isKibanaIndex(params.getOrDefault("kibana_indices", ".kibana"), indexName)) {
            return UpgradeActionRequired.UPGRADE;
        }
        return UpgradeActionRequired.NOT_APPLICABLE;
    }

    private boolean isKibanaIndex(String kibanaIndicesMasks, String indexName) {
        String[] kibanaIndices = Strings.delimitedListToStringArray(kibanaIndicesMasks, ",");
        return Regex.simpleMatch(kibanaIndices, indexName);
    }

    @Override
    public Collection<String> supportedParams() {
        return Collections.singletonList("kibana_indices");
    }
}
