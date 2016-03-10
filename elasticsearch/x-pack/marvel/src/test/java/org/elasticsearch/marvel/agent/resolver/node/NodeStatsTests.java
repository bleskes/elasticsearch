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

package org.elasticsearch.marvel.agent.resolver.node;

import org.apache.lucene.util.Constants;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.MarvelSettings;
import org.elasticsearch.marvel.agent.collector.node.NodeStatsCollector;
import org.elasticsearch.marvel.agent.exporter.local.LocalExporter;
import org.elasticsearch.marvel.agent.resolver.node.NodeStatsResolver;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.junit.After;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;

// numClientNodes is set to 0 because Client nodes don't have Filesystem stats
@ClusterScope(scope = Scope.TEST, numClientNodes = 0, transportClientRatio = 0.0)
public class NodeStatsTests extends MarvelIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL.getKey(), "-1")
                .put(MarvelSettings.COLLECTORS.getKey(), NodeStatsCollector.NAME)
                .put("xpack.monitoring.agent.exporters.default_local.type", LocalExporter.TYPE)
                .build();
    }

    @After
    public void cleanup() throws Exception {
        updateMarvelInterval(-1, TimeUnit.SECONDS);
        wipeMarvelIndices();
    }

    public void testNodeStats() throws Exception {
        logger.debug("--> creating some indices for future node stats");
        final int numDocs = between(50, 150);
        for (int i = 0; i < numDocs; i++) {
            client().prepareIndex("test", "foo").setSource("value", randomInt()).get();
        }

        securedFlush();
        securedRefresh();

        updateMarvelInterval(3L, TimeUnit.SECONDS);
        waitForMarvelIndices();

        awaitMarvelDocsCount(greaterThan(0L), NodeStatsResolver.TYPE);

        logger.debug("--> searching for monitoring documents of type [{}]", NodeStatsResolver.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(NodeStatsResolver.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = NodeStatsResolver.FILTERS;
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                if (Constants.WINDOWS) {
                    // load average is unavailable on Windows
                    if ("node_stats.os.cpu.load_average.1m".equals(filter)) {
                        continue;
                    }
                }
                assertContains(filter, fields);
            }
        }

        logger.debug("--> node stats successfully collected");
    }
}
