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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.Renderer;
import org.elasticsearch.marvel.agent.renderer.RendererTestUtils;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.test.StreamsUtils;
import org.junit.Test;

public class ClusterStateRendererTests extends ESSingleNodeTestCase {

    private static final String SAMPLE_FILE = "/samples/cluster_state.json";

    @Test
    public void testClusterStateRenderer() throws Exception {
        createIndex("my-index", Settings.settingsBuilder()
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 3)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                .build());

        logger.debug("--> retrieving cluster state");
        ClusterState clusterState = getInstanceFromNode(ClusterService.class).state();

        logger.debug("--> retrieving cluster health");
        ClusterHealthResponse clusterHealth = client().admin().cluster().prepareHealth().get();

        logger.debug("--> creating the cluster state marvel document");
        ClusterStateMarvelDoc marvelDoc = new ClusterStateMarvelDoc("test", "cluster_state", 1437580442979L,
                clusterState, clusterHealth.getStatus());

        logger.debug("--> rendering the document");
        Renderer renderer = new ClusterStateRenderer();
        String result = RendererTestUtils.renderAsJSON(marvelDoc, renderer);

        logger.debug("--> loading sample document from file {}", SAMPLE_FILE);
        String expected = StreamsUtils.copyToStringFromClasspath(SAMPLE_FILE);

        String nodeId = clusterState.getNodes().getLocalNodeId();
        logger.debug("--> replace the local node id in sample document with {}", nodeId);
        expected = Strings.replace(expected, "__node_id__", nodeId);

        logger.debug("--> comparing both documents, they must have the same structure");
        RendererTestUtils.assertJSONStructure(result, expected);
    }
}
