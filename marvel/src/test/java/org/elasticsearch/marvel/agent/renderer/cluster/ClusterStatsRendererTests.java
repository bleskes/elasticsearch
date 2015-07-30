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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.Renderer;
import org.elasticsearch.marvel.agent.renderer.RendererTestUtils;
import org.elasticsearch.test.ElasticsearchSingleNodeTest;
import org.junit.Test;

public class ClusterStatsRendererTests extends ElasticsearchSingleNodeTest {

    private static final String SAMPLE_FILE = "/samples/marvel_cluster_stats.json";

    @Test
    public void testClusterStatsRenderer() throws Exception {
        createIndex("index-0");

        logger.debug("--> retrieving cluster stats response");
        ClusterStatsResponse clusterStats = client().admin().cluster().prepareClusterStats().get();

        logger.debug("--> creating the cluster stats marvel document");
        ClusterStatsMarvelDoc marvelDoc = ClusterStatsMarvelDoc.createMarvelDoc("test", "marvel_cluster_stats", 1437580442979L, clusterStats);

        logger.debug("--> rendering the document");
        Renderer renderer = new ClusterStatsRenderer();
        String result = RendererTestUtils.renderAsJSON(marvelDoc, renderer);

        logger.debug("--> loading sample document from file {}", SAMPLE_FILE);
        String expected = Streams.copyToStringFromClasspath(SAMPLE_FILE);

        logger.debug("--> comparing both documents, they must have the same structure");
        RendererTestUtils.assertJSONStructure(result, expected);
    }
}
