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

package org.elasticsearch.xpack.monitoring.resolver.node;

import org.apache.lucene.util.Constants;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.node.NodeStatsCollector;
import org.elasticsearch.xpack.monitoring.exporter.local.LocalExporter;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.junit.After;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThan;

// numClientNodes is set to 0 because Client nodes don't have Filesystem stats
@ClusterScope(scope = Scope.TEST, numClientNodes = 0, transportClientRatio = 0.0)
public class NodeStatsTests extends MonitoringIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MonitoringSettings.INTERVAL.getKey(), "-1")
                .put(MonitoringSettings.COLLECTORS.getKey(), NodeStatsCollector.NAME)
                .put("xpack.monitoring.exporters.default_local.type", LocalExporter.TYPE)
                .build();
    }

    @After
    public void cleanup() throws Exception {
        updateMonitoringInterval(-1, TimeUnit.SECONDS);
        wipeMonitoringIndices();
    }

    public void testNodeStats() throws Exception {
        final int numDocs = between(50, 150);
        for (int i = 0; i < numDocs; i++) {
            client().prepareIndex("test", "foo").setSource("value", randomInt()).get();
        }

        flush();
        refresh();

        updateMonitoringInterval(3L, TimeUnit.SECONDS);
        waitForMonitoringIndices();

        awaitMonitoringDocsCount(greaterThan(0L), NodeStatsResolver.TYPE);

        SearchResponse response = client().prepareSearch().setTypes(NodeStatsResolver.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : nodeStatsFilters(watcherEnabled)) {
                if (Constants.WINDOWS) {
                    // load average is unavailable on Windows
                    if (filter.startsWith("node_stats.os.cpu.load_average")) {
                        continue;
                    }
                }
                // fs stats and cgroup stats are not reported on every node
                if (filter.startsWith("node_stats.fs") || filter.startsWith("node_stats.os.cgroup")) {
                    continue;
                }

                assertContains(filter, fields);
            }
        }
    }

    /**
     * Optionally exclude {@link NodeStatsResolver#FILTERS} that require Watcher to be enabled.
     *
     * @param includeWatcher {@code true} to keep watcher filters.
     * @return Never {@code null} or empty.
     * @see #watcherEnabled
     */
    private static Set<String> nodeStatsFilters(boolean includeWatcher) {
        if (includeWatcher) {
            return NodeStatsResolver.FILTERS;
        }

        return NodeStatsResolver.FILTERS.stream().filter(s -> s.contains("watcher") == false).collect(Collectors.toSet());
    }

    @Override
    protected boolean enableWatcher() {
        // currently this is the only Monitoring test that expects Watcher to be enabled.
        // Once this becomes the default, then this should be removed.
        return randomBoolean();
    }
}
