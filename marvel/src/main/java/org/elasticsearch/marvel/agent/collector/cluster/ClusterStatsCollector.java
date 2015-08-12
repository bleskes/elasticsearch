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
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettingsService;

import java.util.Collection;

/**
 * Collector for cluster stats.
 * <p/>
 * This collector runs on the master node only and collects the {@link ClusterStatsMarvelDoc} document
 * at a given frequency.
 */
public class ClusterStatsCollector extends AbstractCollector<ClusterStatsCollector> {

    public static final String NAME = "cluster-stats-collector";
    public static final String TYPE = "marvel_cluster_stats";

    private final Client client;

    @Inject
    public ClusterStatsCollector(Settings settings, ClusterService clusterService,
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

        ClusterStatsResponse clusterStatsResponse = client.admin().cluster().prepareClusterStats().get(marvelSettings.clusterStatsTimeout());
        results.add(buildMarvelDoc(clusterName.value(), TYPE, System.currentTimeMillis(), clusterStatsResponse));
        return results.build();
    }

    protected MarvelDoc buildMarvelDoc(String clusterName, String type, long timestamp, ClusterStatsResponse clusterStatsResponse) {
        return ClusterStatsMarvelDoc.createMarvelDoc(clusterName, type, timestamp, clusterStatsResponse);
    }
}
