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

package org.elasticsearch.xpack.monitoring.exporter.local;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.ingest.PipelineConfiguration;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkDoc;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkRequestBuilder;
import org.elasticsearch.xpack.monitoring.action.MonitoringIndex;
import org.elasticsearch.xpack.monitoring.exporter.ClusterAlertsUtil;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.transport.actions.get.GetWatchRequest;
import org.elasticsearch.xpack.watcher.transport.actions.get.GetWatchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.search.aggregations.AggregationBuilders.max;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.xpack.monitoring.MonitoredSystem.KIBANA;
import static org.elasticsearch.xpack.monitoring.MonitoredSystem.LOGSTASH;
import static org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils.OLD_TEMPLATE_VERSION;
import static org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils.PIPELINE_IDS;
import static org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils.TEMPLATE_VERSION;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.SUITE,
                              numDataNodes = 1, numClientNodes = 0, transportClientRatio = 0.0, supportsDedicatedMasters = false)
public class LocalExporterIntegTests extends LocalExporterIntegTestCase {

    private final String indexTimeFormat = randomFrom("YY", "YYYY", "YYYY.MM", "YYYY-MM", "MM.YYYY", "MM", null);

    private void stopMonitoring() throws Exception {
        // Now disabling the monitoring service, so that no more collection are started
        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(
                Settings.builder().putNull(MonitoringSettings.INTERVAL.getKey())
                                  .putNull("xpack.monitoring.exporters._local.enabled")
                                  .putNull("xpack.monitoring.exporters._local.index.name.time_format")));
    }

    public void testExport() throws Exception {
        try {
            if (randomBoolean()) {
                // indexing some random documents
                IndexRequestBuilder[] indexRequestBuilders = new IndexRequestBuilder[5];
                for (int i = 0; i < indexRequestBuilders.length; i++) {
                    indexRequestBuilders[i] = client().prepareIndex("test", "type", Integer.toString(i))
                            .setSource("title", "This is a random document");
                }
                indexRandom(true, indexRequestBuilders);
            }

            if (randomBoolean()) {
                // create some marvel indices to check if aliases are correctly created
                final int oldies = randomIntBetween(1, 5);
                for (int i = 0; i < oldies; i++) {
                    assertAcked(client().admin().indices().prepareCreate(".marvel-es-1-2014.12." + i)
                            .setSettings("number_of_shards", 1, "number_of_replicas", 0).get());
                }
            }

            Settings.Builder exporterSettings = Settings.builder()
                .put("xpack.monitoring.exporters._local.enabled", true);

            if (indexTimeFormat != null) {
                exporterSettings.put("xpack.monitoring.exporters._local.index.name.time_format", indexTimeFormat);
            }

            // local exporter is now enabled
            assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(exporterSettings));

            if (randomBoolean()) {
                // export some documents now, before starting the monitoring service
                final int nbDocs = randomIntBetween(1, 20);
                List<MonitoringBulkDoc> monitoringDocs = new ArrayList<>(nbDocs);
                for (int i = 0; i < nbDocs; i++) {
                    monitoringDocs.add(createMonitoringBulkDoc(String.valueOf(i)));
                }

                assertBusy(() -> {
                    MonitoringBulkRequestBuilder bulk = monitoringClient().prepareMonitoringBulk();
                    monitoringDocs.forEach(bulk::add);
                    assertEquals(RestStatus.OK, bulk.get().status());
                    refresh();

                    assertThat(client().admin().indices().prepareExists(".monitoring-*").get().isExists(), is(true));
                    ensureYellow(".monitoring-*");

                    SearchResponse response = client().prepareSearch(".monitoring-*").get();
                    assertEquals(nbDocs, response.getHits().getTotalHits());
                });

                checkMonitoringTemplates();
                checkMonitoringPipelines();
                checkMonitoringAliases();
                checkMonitoringDocs();
            }

            // monitoring service is started
            exporterSettings = Settings.builder()
                    .put(MonitoringSettings.INTERVAL.getKey(), 3L, TimeUnit.SECONDS);
            assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(exporterSettings));

            final int numNodes = internalCluster().getNodeNames().length;
            assertBusy(() -> {
                assertThat(client().admin().indices().prepareExists(".monitoring-*").get().isExists(), is(true));
                ensureYellow(".monitoring-*");

                assertThat(client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "cluster_stats"))
                        .get().getHits().getTotalHits(), greaterThan(0L));

                assertThat(client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "index_recovery"))
                        .get().getHits().getTotalHits(), greaterThan(0L));

                assertThat(client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "index_stats"))
                        .get().getHits().getTotalHits(), greaterThan(0L));

                assertThat(client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "indices_stats"))
                        .get().getHits().getTotalHits(), greaterThan(0L));

                assertThat(client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "shards"))
                        .get().getHits().getTotalHits(), greaterThan(0L));

                SearchResponse response = client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "node_stats"))
                        .addAggregation(terms("agg_nodes_ids").field("node_stats.node_id"))
                        .get();

                Terms aggregation = response.getAggregations().get("agg_nodes_ids");
                assertEquals("Aggregation on node_id must return a bucket per node involved in test",
                        numNodes, aggregation.getBuckets().size());

                for (String nodeName : internalCluster().getNodeNames()) {
                    String nodeId = internalCluster().clusterService(nodeName).localNode().getId();
                    Terms.Bucket bucket = aggregation.getBucketByKey(nodeId);
                    assertTrue("No bucket found for node id [" + nodeId + "]", bucket != null);
                    assertTrue(bucket.getDocCount() >= 1L);
                }

            }, 30L, TimeUnit.SECONDS);

            checkMonitoringTemplates();
            checkMonitoringPipelines();
            checkMonitoringAliases();
            checkMonitoringWatches();
            checkMonitoringDocs();
        } finally {
            stopMonitoring();
        }

        // This assertion loop waits for in flight exports to terminate. It checks that the latest
        // node_stats document collected for each node is at least 10 seconds old, corresponding to
        // 2 or 3 elapsed collection intervals.
        final int elapsedInSeconds = 10;
        final DateTime startTime = DateTime.now(DateTimeZone.UTC);
        assertBusy(() -> {
            IndicesExistsResponse indicesExistsResponse = client().admin().indices().prepareExists(".monitoring-*").get();
            if (indicesExistsResponse.isExists()) {
                ensureYellow(".monitoring-*");
                refresh(".monitoring-es-*");

                SearchResponse response = client().prepareSearch(".monitoring-es-*")
                        .setSize(0)
                        .setQuery(QueryBuilders.termQuery("type", "node_stats"))
                        .addAggregation(terms("agg_nodes_ids").field("node_stats.node_id")
                                .subAggregation(max("agg_last_time_collected").field("timestamp")))
                        .get();

                Terms aggregation = response.getAggregations().get("agg_nodes_ids");
                for (String nodeName : internalCluster().getNodeNames()) {
                    String nodeId = internalCluster().clusterService(nodeName).localNode().getId();
                    Terms.Bucket bucket = aggregation.getBucketByKey(nodeId);
                    assertTrue("No bucket found for node id [" + nodeId + "]", bucket != null);
                    assertTrue(bucket.getDocCount() >= 1L);

                    Max subAggregation = bucket.getAggregations().get("agg_last_time_collected");
                    DateTime lastCollection = new DateTime(Math.round(subAggregation.getValue()), DateTimeZone.UTC);
                    assertTrue(lastCollection.plusSeconds(elapsedInSeconds).isBefore(DateTime.now(DateTimeZone.UTC)));
                }
            } else {
                assertTrue(DateTime.now(DateTimeZone.UTC).isAfter(startTime.plusSeconds(elapsedInSeconds)));
            }
        }, 30L, TimeUnit.SECONDS);
    }

    /**
     * Checks that the monitoring templates have been created by the local exporter
     */
    private void checkMonitoringTemplates() {
        final Set<String> templates = new HashSet<>();
        templates.add(".monitoring-alerts");
        for (MonitoredSystem system : MonitoredSystem.values()) {
            templates.add(String.join("-", ".monitoring", system.getSystem()));
        }

        GetIndexTemplatesResponse response =
                client().admin().indices().prepareGetTemplates(".monitoring-*").get();
        Set<String> actualTemplates = response.getIndexTemplates().stream()
                .map(IndexTemplateMetaData::getName).collect(Collectors.toSet());
        assertEquals(templates, actualTemplates);
    }

    /**
     * Checks that the monitoring ingest pipelines have been created by the local exporter
     */
    private void checkMonitoringPipelines() {
        final Set<String> expectedPipelines =
                Arrays.stream(PIPELINE_IDS).map(MonitoringTemplateUtils::pipelineName).collect(Collectors.toSet());

        final GetPipelineResponse response = client().admin().cluster().prepareGetPipeline("xpack_monitoring_*").get();

        // actual pipelines
        final Set<String> pipelines = response.pipelines().stream().map(PipelineConfiguration::getId).collect(Collectors.toSet());

        assertEquals("Missing expected pipelines", expectedPipelines, pipelines);
        assertTrue("monitoring ingest pipeline not found", response.isFound());
    }

    /**
     * Checks that the local exporter correctly added aliases to indices created with previous
     * Marvel versions.
     */
    private void checkMonitoringAliases() {
        GetIndexResponse response =
                client().admin().indices().prepareGetIndex().setIndices(".marvel-es-1-*").get();
        for (String index : response.getIndices()) {
            List<AliasMetaData> aliases = response.getAliases().get(index);
            assertEquals("marvel index should have at least 1 alias: " + index, 1, aliases.size());

            String indexDate = index.substring(".marvel-es-1-".length());
            String expectedAlias = ".monitoring-es-" + OLD_TEMPLATE_VERSION + "-" + indexDate + "-alias";
            assertEquals(expectedAlias, aliases.get(0).getAlias());
        }
    }

    /**
     * Checks that the local exporter correctly creates Watches.
     */
    private void checkMonitoringWatches() throws ExecutionException, InterruptedException {
        if (enableWatcher()) {
            final XPackClient xpackClient = new XPackClient(client());
            final WatcherClient watcher = xpackClient.watcher();

            for (final String watchId : ClusterAlertsUtil.WATCH_IDS) {
                final String uniqueWatchId = ClusterAlertsUtil.createUniqueWatchId(clusterService(), watchId);
                final GetWatchResponse response = watcher.getWatch(new GetWatchRequest(uniqueWatchId)).get();

                assertTrue("watch [" + watchId + "] should exist", response.isFound());
            }
        }
    }

    /**
     * Checks that the monitoring documents all have the cluster_uuid, timestamp and source_node
     * fields and belongs to the right data or timestamped index.
     */
    private void checkMonitoringDocs() {
        ClusterStateResponse response = client().admin().cluster().prepareState().get();
        String customTimeFormat = response.getState().getMetaData().transientSettings()
                .get("xpack.monitoring.exporters._local.index.name.time_format");
        assertEquals(indexTimeFormat, customTimeFormat);
        if (customTimeFormat == null) {
            customTimeFormat = "YYYY.MM.dd";
        }

        DateTimeFormatter dateParser = ISODateTimeFormat.dateTime().withZoneUTC();
        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(customTimeFormat).withZoneUTC();

        SearchResponse searchResponse = client().prepareSearch(".monitoring-*").setSize(100).get();
        assertThat(searchResponse.getHits().getTotalHits(), greaterThan(0L));

        for (SearchHit hit : searchResponse.getHits().getHits()) {
            final Map<String, Object> source = hit.getSourceAsMap();

            assertTrue(source != null && source.isEmpty() == false);

            final String timestamp = (String) source.get("timestamp");
            final String type = (String) source.get("type");

            assertTrue("document is missing cluster_uuid field", Strings.hasText((String) source.get("cluster_uuid")));
            assertTrue("document is missing timestamp field", Strings.hasText(timestamp));
            assertTrue("document is missing type field", Strings.hasText(type));
            assertEquals("document _type is 'doc'", "doc", hit.getType());

            @SuppressWarnings("unchecked")
            Map<String, Object> docSource = (Map<String, Object>) source.get("doc");

            MonitoredSystem expectedSystem;
            if (docSource == null) {
                // This is a document indexed by the Monitoring service
                expectedSystem = MonitoredSystem.ES;
            } else {
                // This is a document indexed through the Monitoring Bulk API
                expectedSystem = MonitoredSystem.fromSystem((String) docSource.get("expected_system"));
            }

            Set<String> expectedIndex = new HashSet<>();

            String dateTime = dateFormatter.print(dateParser.parseDateTime(timestamp));
            expectedIndex.add(".monitoring-" + expectedSystem.getSystem() + "-" + TEMPLATE_VERSION + "-" + dateTime);

            assertTrue("Expected " + expectedIndex + " but got " + hit.getIndex(), expectedIndex.contains(hit.getIndex()));

            @SuppressWarnings("unchecked")
            Map<String, Object> sourceNode = (Map<String, Object>) source.get("source_node");
            if ("shards".equals(type) == false) {
                assertNotNull("document is missing source_node field", sourceNode);
            }
        }
    }

    private static MonitoringBulkDoc createMonitoringBulkDoc(String id) throws IOException {
        String monitoringId = randomFrom(KIBANA, LOGSTASH).getSystem();
        String monitoringVersion = TEMPLATE_VERSION;
        XContentType xContentType = randomFrom(XContentType.values());

        BytesReference source;

        try (XContentBuilder builder = XContentBuilder.builder(xContentType.xContent())) {
            builder.startObject();
            {
                builder.field("expected_system", monitoringId);
                final int nbFields = randomIntBetween(1, 3);
                for (int i = 0; i < nbFields; i++) {
                    builder.field("field_" + i, i);
                }
            }
            builder.endObject();
            source = builder.bytes();
        }

        return new MonitoringBulkDoc(monitoringId, monitoringVersion, MonitoringIndex.TIMESTAMPED, "doc", id, source, xContentType);
    }

}
