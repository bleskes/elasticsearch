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

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.marvel.MarvelSettings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MonitoringDoc;
import org.elasticsearch.marvel.license.MarvelLicensee;
import org.elasticsearch.shield.InternalClient;
import org.elasticsearch.shield.Shield;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Collector for indices statistics.
 * <p>
 * This collector runs on the master node only and collect one {@link IndicesStatsMonitoringDoc} document.
 */
public class IndicesStatsCollector extends AbstractCollector<IndicesStatsCollector> {

    public static final String NAME = "indices-stats-collector";

    private final Client client;

    @Inject
    public IndicesStatsCollector(Settings settings, ClusterService clusterService,
                                 MarvelSettings  marvelSettings, MarvelLicensee marvelLicensee, InternalClient client) {
        super(settings, NAME, clusterService, marvelSettings, marvelLicensee);
        this.client = client;
    }

    @Override
    protected boolean shouldCollect() {
        return super.shouldCollect() && isLocalNodeMaster();
    }

    @Override
    protected Collection<MonitoringDoc> doCollect() throws Exception {
        try {
            IndicesStatsResponse indicesStats = client.admin().indices().prepareStats()
                    .setIndices(marvelSettings.indices())
                    .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .clear()
                    .setDocs(true)
                    .setIndexing(true)
                    .setSearch(true)
                    .setStore(true)
                    .get(marvelSettings.indicesStatsTimeout());

            IndicesStatsMonitoringDoc indicesStatsDoc = new IndicesStatsMonitoringDoc(monitoringId(), monitoringVersion());
            indicesStatsDoc.setClusterUUID(clusterUUID());
            indicesStatsDoc.setTimestamp(System.currentTimeMillis());
            indicesStatsDoc.setSourceNode(localNode());
            indicesStatsDoc.setIndicesStats(indicesStats);

            return Collections.singletonList(indicesStatsDoc);
        } catch (IndexNotFoundException e) {
            if (Shield.enabled(settings) && IndexNameExpressionResolver.isAllIndices(Arrays.asList(marvelSettings.indices()))) {
                logger.debug("collector [{}] - unable to collect data for missing index [{}]", name(), e.getIndex());
                return Collections.emptyList();
            }
            throw e;
        }
    }
}
