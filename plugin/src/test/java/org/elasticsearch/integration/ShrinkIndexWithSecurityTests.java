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

package org.elasticsearch.integration;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.SecurityIntegTestCase;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;

/**
 * Integration test that uses multiple data nodes to test that the shrink index api works with security.
 */
@ClusterScope(minNumDataNodes = 2)
public class ShrinkIndexWithSecurityTests extends SecurityIntegTestCase {

    @Override
    protected final boolean ignoreExternalCluster() {
        return true;
    }

    @Override
    protected int minimumNumberOfShards() {
        return 2;
    }

    public void testShrinkIndex() throws Exception {
        final int randomNumberOfDocs = scaledRandomIntBetween(2, 12);
        for (int i = 0; i < randomNumberOfDocs; i++) {
            client().prepareIndex("bigindex", "type").setSource("foo", "bar").get();
        }

        ImmutableOpenMap<String, DiscoveryNode> dataNodes = client().admin().cluster().prepareState().get().getState().nodes()
                .getDataNodes();
        DiscoveryNode[] discoveryNodes = dataNodes.values().toArray(DiscoveryNode.class);
        final String mergeNode = discoveryNodes[0].getName();
        ensureGreen();
        // relocate all shards to one node such that we can merge it.
        client().admin().indices().prepareUpdateSettings("bigindex")
                .setSettings(Settings.builder()
                        .put("index.routing.allocation.require._name", mergeNode)
                        .put("index.blocks.write", true)).get();

        // wait for green and then shrink
        ensureGreen();
        assertAcked(client().admin().indices().prepareShrinkIndex("bigindex", "shrunk_bigindex")
                .setSettings(Settings.builder()
                        .put("index.number_of_replicas", 0)
                        .put("index.number_of_shards", 1)
                        .build()));

        // verify all docs
        ensureGreen();
        assertHitCount(client().prepareSearch("shrunk_bigindex").setSize(100).setQuery(new TermsQueryBuilder("foo", "bar")).get(),
                randomNumberOfDocs);
    }
}
