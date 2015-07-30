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

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.*;

public class ClusterStatsCollectorTests extends ElasticsearchIntegrationTest {

    @Test
    public void testClusterStatsCollector() throws Exception {
        Collection<MarvelDoc> results = newClusterStatsCollector().doCollect();
        assertThat(results, hasSize(1));

        MarvelDoc marvelDoc = results.iterator().next();
        assertNotNull(marvelDoc);
        assertThat(marvelDoc, instanceOf(ClusterStatsMarvelDoc.class));

        ClusterStatsMarvelDoc clusterStatsMarvelDoc = (ClusterStatsMarvelDoc) marvelDoc;
        assertThat(clusterStatsMarvelDoc.clusterName(), equalTo(client().admin().cluster().prepareHealth().get().getClusterName()));
        assertThat(clusterStatsMarvelDoc.timestamp(), greaterThan(0L));
        assertThat(clusterStatsMarvelDoc.type(), equalTo(ClusterStatsCollector.TYPE));

        ClusterStatsMarvelDoc.Payload payload = clusterStatsMarvelDoc.payload();
        assertNotNull(payload);
        assertNotNull(payload.getClusterStats());
        assertThat(payload.getClusterStats().getNodesStats().getCounts().getTotal(), equalTo(internalCluster().getNodeNames().length));
    }

    private ClusterStatsCollector newClusterStatsCollector() {
        return new ClusterStatsCollector(internalCluster().getInstance(Settings.class), internalCluster().getInstance(ClusterService.class),
                internalCluster().getInstance(ClusterName.class), client());
    }
}
