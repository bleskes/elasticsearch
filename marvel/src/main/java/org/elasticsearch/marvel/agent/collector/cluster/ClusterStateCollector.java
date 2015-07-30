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

package org.elasticsearch.marvel.agent.collector.cluster;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettingsService;

import java.util.Collection;

/**
 * Collector for cluster state.
 * <p/>
 * This collector runs on the master node only and collects the {@link ClusterStateMarvelDoc} document
 * at a given frequency.
 */
public class ClusterStateCollector extends AbstractCollector<ClusterStateCollector> {

    public static final String NAME = "cluster-state-collector";
    public static final String TYPE = "marvel_cluster_state";

    private final Client client;

    @Inject
    public ClusterStateCollector(Settings settings, ClusterService clusterService,
                                 ClusterName clusterName, MarvelSettingsService marvelSettings, Client client) {
        super(settings, NAME, clusterService, clusterName, marvelSettings);
        this.client = client;
    }

    @Override
    protected boolean masterOnly() {
        return true;
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        ImmutableList.Builder<MarvelDoc> results = ImmutableList.builder();

        ClusterState clusterState = clusterService.state();
        ClusterHealthResponse clusterHealth = client.admin().cluster().prepareHealth().get(marvelSettings.clusterStateTimeout());

        results.add(buildMarvelDoc(clusterName.value(), TYPE, System.currentTimeMillis(), clusterState, clusterHealth.getStatus()));
        return results.build();
    }

    protected MarvelDoc buildMarvelDoc(String clusterName, String type, long timestamp, ClusterState clusterState, ClusterHealthStatus status) {
        return ClusterStateMarvelDoc.createMarvelDoc(clusterName, type, timestamp, clusterState, status);
    }
}
