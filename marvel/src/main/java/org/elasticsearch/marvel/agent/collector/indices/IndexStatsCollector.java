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

package org.elasticsearch.marvel.agent.collector.indices;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

import java.util.Collection;

/**
 * Collector for indices statistics.
 *
 * This collector runs on the master node only and collect a {@link IndexStatsMarvelDoc} document
 * for each existing index in the cluster.
 */
public class IndexStatsCollector extends AbstractCollector<IndexStatsCollector> {

    public static final String NAME = "index-stats-collector";
    protected static final String TYPE = "marvel_index";

    private final ClusterName clusterName;
    private final Client client;

    @Inject
    public IndexStatsCollector(Settings settings, ClusterService clusterService, ClusterName clusterName, Client client) {
        super(settings, NAME, clusterService);
        this.client = client;
        this.clusterName = clusterName;
    }

    @Override
    protected boolean masterOnly() {
        return true;
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        ImmutableList.Builder<MarvelDoc> results = ImmutableList.builder();

        IndicesStatsResponse indicesStats = client.admin().indices().prepareStats().all()
                .setStore(true)
                .setIndexing(true)
                .setDocs(true)
                .get();

        long timestamp = System.currentTimeMillis();
        for (IndexStats indexStats : indicesStats.getIndices().values()) {
            results.add(buildMarvelDoc(clusterName.value(), TYPE, timestamp, indexStats));
        }
        return results.build();
    }

    protected MarvelDoc buildMarvelDoc(String clusterName, String type, long timestamp, IndexStats indexStats) {
        return IndexStatsMarvelDoc.createMarvelDoc(clusterName, type, timestamp,
                indexStats.getIndex(),
                indexStats.getTotal().getDocs().getCount(),
                indexStats.getTotal().getStore().sizeInBytes(), indexStats.getTotal().getStore().throttleTime().millis(),
                indexStats.getTotal().getIndexing().getTotal().getThrottleTimeInMillis());
    }
}
