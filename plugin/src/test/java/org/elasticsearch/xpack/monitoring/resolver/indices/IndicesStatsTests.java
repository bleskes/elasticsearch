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

package org.elasticsearch.xpack.monitoring.resolver.indices;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.junit.After;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;

@ClusterScope(scope = Scope.TEST, numClientNodes = 0)
public class IndicesStatsTests extends MonitoringIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MonitoringSettings.INTERVAL.getKey(), "-1")
                .put("xpack.monitoring.exporters.default_local.type", "local")
                .build();
    }

    @After
    public void cleanup() throws Exception {
        disableMonitoringInterval();
        wipeMonitoringIndices();
    }

    public void testIndicesStats() throws Exception {
        logger.debug("--> creating some indices for future indices stats");
        final int nbIndices = randomIntBetween(1, 5);
        for (int i = 0; i < nbIndices; i++) {
            createIndex("stat-" + i);
        }

        final long[] nbDocsPerIndex = new long[nbIndices];
        for (int i = 0; i < nbIndices; i++) {
            nbDocsPerIndex[i] = randomIntBetween(1, 50);
            for (int j = 0; j < nbDocsPerIndex[i]; j++) {
                client().prepareIndex("stat-" + i, "type1").setSource("num", i).get();
            }
        }

        logger.debug("--> wait for indices stats collector to collect stats for all primaries shards");
        assertBusy(new Runnable() {
            @Override
            public void run() {
                flush();
                refresh();

                for (int i = 0; i < nbIndices; i++) {
                    IndicesStatsResponse indicesStats = client().admin().indices().prepareStats().get();
                    assertThat(indicesStats.getPrimaries().getDocs().getCount(), greaterThan(0L));
                }
            }
        });

        updateMonitoringInterval(3L, TimeUnit.SECONDS);
        waitForMonitoringIndices();

        logger.debug("--> wait for indices stats collector to collect global stat");
        awaitMonitoringDocsCountOnPrimary(greaterThan(0L), IndicesStatsResolver.TYPE);

        logger.debug("--> searching for monitoring documents of type [{}]", IndicesStatsResolver.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(IndicesStatsResolver.TYPE).setPreference("_primary").get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.getSourceAsMap();

            for (String filter : IndicesStatsResolver.FILTERS) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> indices stats successfully collected");
    }
}
