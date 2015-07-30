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

package org.elasticsearch.marvel.agent.renderer.node;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.marvel.agent.collector.node.NodeStatsMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.Renderer;
import org.elasticsearch.marvel.agent.renderer.RendererTestUtils;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.test.ElasticsearchSingleNodeTest;
import org.junit.Test;

public class NodeStatsRendererTests extends ElasticsearchSingleNodeTest {

    private static final String SAMPLE_FILE = "/samples/marvel_node_stats.json";

    @Test
    @AwaitsFix(bugUrl = "https://github.com/elastic/x-plugins/issues/367")
    public void testNodeStatsRenderer() throws Exception {
        createIndex("index-0");

        logger.debug("--> retrieving node stats");
        NodeStats nodeStats = getInstanceFromNode(NodeService.class).stats();

        logger.debug("--> creating the node stats marvel document");
        NodeStatsMarvelDoc marvelDoc = NodeStatsMarvelDoc.createMarvelDoc("test", "marvel_node_stats", 1437580442979L,
                "node-0", true, nodeStats, false, 90.0, true);

        logger.debug("--> rendering the document");
        Renderer renderer = new NodeStatsRenderer();
        String result = RendererTestUtils.renderAsJSON(marvelDoc, renderer);

        logger.debug("--> loading sample document from file {}", SAMPLE_FILE);
        String expected = Streams.copyToStringFromClasspath(SAMPLE_FILE);

        logger.debug("--> comparing both documents, they must have the same structure");
        RendererTestUtils.assertJSONStructure(result, expected);
    }
}