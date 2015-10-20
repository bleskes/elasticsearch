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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsCollector;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.junit.After;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.greaterThan;

@ClusterScope(scope = Scope.TEST, numClientNodes = 0)
public class ClusterStatsTests extends MarvelIntegTestCase {
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL, "-1")
                .put(MarvelSettings.COLLECTORS, ClusterStatsCollector.NAME)
                .put("marvel.agent.exporters.default_local.type", "local")
                .put("marvel.agent.exporters.default_local.template.settings.index.number_of_replicas", 0)
                .build();
    }

    @After
    public void cleanup() throws Exception {
        updateMarvelInterval(-1, TimeUnit.SECONDS);
        wipeMarvelIndices();
    }

    public void testClusterStats() throws Exception {
        logger.debug("--> creating some indices so that every data nodes will at least a shard");
        ClusterStatsNodes.Counts counts = client().admin().cluster().prepareClusterStats().get().getNodesStats().getCounts();
        assertThat(counts.getTotal(), greaterThan(0));

        String indexNameBase = randomAsciiOfLength(5).toLowerCase(Locale.ROOT);
        int indicesCount = randomIntBetween(1, 5);
        String[] indices = new String[indicesCount];
        for (int i = 0; i < indicesCount; i++) {
            indices[i] = indexNameBase + "-" + i;
            index(indices[i], "foo", "1", jsonBuilder().startObject().field("dummy_field", 1).endObject());
        }

        securedFlush();
        securedRefresh();
        securedEnsureGreen();

        // ok.. we'll start collecting now...
        updateMarvelInterval(3L, TimeUnit.SECONDS);

        awaitMarvelTemplateInstalled();

        assertBusy(new Runnable() {
            @Override
            public void run() {
                logger.debug("--> searching for marvel [{}] documents", ClusterStatsCollector.TYPE);
                SearchResponse response = client().prepareSearch().setTypes(ClusterStatsCollector.TYPE).get();
                assertThat(response.getHits().getTotalHits(), greaterThan(0L));

                logger.debug("--> checking that every document contains the expected fields");
                String[] filters = ClusterStatsRenderer.FILTERS;
                for (SearchHit searchHit : response.getHits().getHits()) {
                    Map<String, Object> fields = searchHit.sourceAsMap();

                    for (String filter : filters) {
                        assertContains(filter, fields);
                    }
                }

                logger.debug("--> cluster stats successfully collected");
            }
        });
    }
}
