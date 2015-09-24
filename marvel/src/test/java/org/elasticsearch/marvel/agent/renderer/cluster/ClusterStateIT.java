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

package org.elasticsearch.marvel.agent.renderer.cluster;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateCollector;
import org.elasticsearch.marvel.agent.exporter.http.HttpExporterUtils;
import org.elasticsearch.marvel.agent.renderer.AbstractRendererTestCase;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.greaterThan;

public class ClusterStateIT extends AbstractRendererTestCase {

    @Override
    protected Collection<String> collectors() {
        return Collections.singletonList(ClusterStateCollector.NAME);
    }

    @Test
    public void testClusterState() throws Exception {
        awaitMarvelDocsCount(greaterThan(0L), ClusterStateCollector.TYPE);

        logger.debug("--> searching for marvel documents of type [{}]", ClusterStateCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(ClusterStateCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = ClusterStateRenderer.FILTERS;
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> cluster state successfully collected");
    }

    /**
     * This test should fail if the mapping for the 'nodes' attribute
     * in the 'cluster_state' document is NOT set to 'enable: false'
     *
     * See
     */
    @Test
    public void testNoNodesIndexing() throws Exception {
        logger.debug("--> forcing marvel's index template update");
        assertAcked(client().admin().indices().preparePutTemplate("marvel").setSource(HttpExporterUtils.loadDefaultTemplate()).execute().actionGet());

        logger.debug("--> deleting all marvel indices");
        cluster().wipeIndices(MarvelSettings.MARVEL_INDICES_PREFIX + "*");

        logger.debug("--> checking for template existence");
        assertMarvelTemplateExists();
        awaitMarvelDocsCount(greaterThan(0L), ClusterStateCollector.TYPE);

        logger.debug("--> searching for marvel documents of type [{}]", ClusterStateCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(ClusterStateCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        DiscoveryNodes nodes = client().admin().cluster().prepareState().clear().setNodes(true).get().getState().nodes();

        logger.debug("--> ensure that the 'nodes' attributes of the cluster state document is not indexed");
        assertHitCount(client().prepareCount()
                .setTypes(ClusterStateCollector.TYPE)
                .setQuery(QueryBuilders.matchQuery("cluster_state.nodes." + nodes.masterNodeId() + ".name", nodes.masterNode().name())).get(), 0L);
    }
}
