/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */

package org.elasticsearch.xpack.upgrade;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.indices.InvalidIndexNameException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.MockScriptPlugin;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.elasticsearch.test.VersionUtils.randomVersionBetween;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertThrows;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;

public class InternalIndexReindexerIT extends IndexUpgradeIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(XPackPlugin.class, ReindexPlugin.class, CustomScriptPlugin.class);
    }

    public static class CustomScriptPlugin extends MockScriptPlugin {
        @Override
        protected Map<String, Function<Map<String, Object>, Object>> pluginScripts() {
            Map<String, Function<Map<String, Object>, Object>> scripts = new HashMap<>();
            scripts.put("add_bar", map -> {
                @SuppressWarnings("unchecked") Map<String, Object> ctx = (Map<String, Object>) map.get("ctx");
                ctx.put("_id", "bar" + "-" + ctx.get("_id"));
                @SuppressWarnings("unchecked") Map<String, Object> source = (Map<String, Object>) ctx.get("_source");
                source.put("bar", true);
                return null;
            });
            scripts.put("fail", map -> {
                throw new RuntimeException("Stop reindexing");
            });
            return scripts;
        }
    }

    public void testUpgradeIndex() throws Exception {
        createTestIndex("test");
        InternalIndexReindexer reindexer = createIndexReindexer(123, script("add_bar"), Strings.EMPTY_ARRAY);
        PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
        reindexer.upgrade("test", clusterState(), future);
        BulkByScrollResponse response = future.actionGet();
        assertThat(response.getCreated(), equalTo(2L));

        SearchResponse searchResponse = client().prepareSearch("test_v123").get();
        assertThat(searchResponse.getHits().getTotalHits(), equalTo(2L));
        assertThat(searchResponse.getHits().getHits().length, equalTo(2));
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            assertThat(hit.getId(), startsWith("bar-"));
            assertThat(hit.getSourceAsMap(), notNullValue());
            assertThat(hit.getSourceAsMap().get("bar"), equalTo(true));
        }

        GetAliasesResponse aliasesResponse = client().admin().indices().prepareGetAliases("test").get();
        assertThat(aliasesResponse.getAliases().size(), equalTo(1));
        List<AliasMetaData> testAlias = aliasesResponse.getAliases().get("test_v123");
        assertNotNull(testAlias);
        assertThat(testAlias.size(), equalTo(1));
        assertThat(testAlias.get(0).alias(), equalTo("test"));
    }

    public void testTargetIndexExists() throws Exception {
        createTestIndex("test");
        createTestIndex("test_v123");
        InternalIndexReindexer reindexer = createIndexReindexer(123, script("add_bar"), Strings.EMPTY_ARRAY);
        PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
        reindexer.upgrade("test", clusterState(), future);
        assertThrows(future, ResourceAlreadyExistsException.class);

        // Make sure that the index is not marked as read-only
        client().prepareIndex("test", "doc").setSource("foo", "bar").get();
    }

    public void testTargetIndexExistsAsAlias() throws Exception {
        createTestIndex("test");
        createTestIndex("test-foo");
        client().admin().indices().prepareAliases().addAlias("test-foo", "test_v123").get();
        InternalIndexReindexer reindexer = createIndexReindexer(123, script("add_bar"), Strings.EMPTY_ARRAY);
        PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
        reindexer.upgrade("test", clusterState(), future);
        assertThrows(future, InvalidIndexNameException.class);

        // Make sure that the index is not marked as read-only
        client().prepareIndex("test_v123", "doc").setSource("foo", "bar").get();
    }

    public void testSourceIndexIsReadonly() throws Exception {
        createTestIndex("test");
        try {
            Settings settings = Settings.builder().put(IndexMetaData.INDEX_READ_ONLY_SETTING.getKey(), true).build();
            assertAcked(client().admin().indices().prepareUpdateSettings("test").setSettings(settings).get());
            InternalIndexReindexer reindexer = createIndexReindexer(123, script("add_bar"), Strings.EMPTY_ARRAY);
            PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
            reindexer.upgrade("test", clusterState(), future);
            assertThrows(future, IllegalStateException.class);

            // Make sure that the index is still marked as read-only
            assertThrows(client().prepareIndex("test", "doc").setSource("foo", "bar"), ClusterBlockException.class);
        } finally {
            // Clean up the readonly index
            Settings settings = Settings.builder().put(IndexMetaData.INDEX_READ_ONLY_SETTING.getKey(), false).build();
            assertAcked(client().admin().indices().prepareUpdateSettings("test").setSettings(settings).get());
        }
    }


    public void testReindexingFailure() throws Exception {
        createTestIndex("test");
        // Make sure that the index is not marked as read-only
        client().prepareIndex("test", "doc").setSource("foo", "bar").setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
        InternalIndexReindexer reindexer = createIndexReindexer(123, script("fail"), Strings.EMPTY_ARRAY);
        PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
        reindexer.upgrade("test", clusterState(), future);
        assertThrows(future, RuntimeException.class);

        // Make sure that the index is not marked as read-only
        client().prepareIndex("test", "doc").setSource("foo", "bar").get();
    }

    public void testMixedNodeVersion() throws Exception {
        createTestIndex("test");

        InternalIndexReindexer reindexer = createIndexReindexer(123, script("add_bar"), Strings.EMPTY_ARRAY);
        PlainActionFuture<BulkByScrollResponse> future = PlainActionFuture.newFuture();
        reindexer.upgrade("test", withRandomOldNode(), future);
        assertThrows(future, IllegalStateException.class);

        // Make sure that the index is not marked as read-only
        client().prepareIndex("test_v123", "doc").setSource("foo", "bar").get();
    }

    private void createTestIndex(String indexName) throws Exception {
        assertAcked(client().admin().indices().prepareCreate(indexName).get());
        indexRandom(true,
                client().prepareIndex(indexName, "doc", "1").setSource("{\"foo\":\"bar1-1\"}", XContentType.JSON),
                client().prepareIndex(indexName, "doc", "2").setSource("{\"foo\":\"baz1-1\"}", XContentType.JSON)
        );
        ensureYellow(indexName);
    }

    private Script script(String name) {
        return new Script(ScriptType.INLINE, CustomScriptPlugin.NAME, name, new HashMap<>());
    }

    private InternalIndexReindexer createIndexReindexer(int version, Script transformScript, String[] types) {
        return new InternalIndexReindexer<Void>(client(), internalCluster().clusterService(internalCluster().getMasterName()),
                version, transformScript, types, voidActionListener -> voidActionListener.onResponse(null),
                (aVoid, listener) -> listener.onResponse(TransportResponse.Empty.INSTANCE));

    }

    private ClusterState clusterState() {
        return clusterService().state();
    }

    private ClusterState withRandomOldNode() {
        ClusterState clusterState = clusterState();
        DiscoveryNodes discoveryNodes = clusterState.nodes();
        List<String> nodes = new ArrayList<>();
        for (ObjectCursor<String> key : discoveryNodes.getMasterAndDataNodes().keys()) {
            nodes.add(key.value);
        }
        // Fake one of the node versions
        String nodeId = randomFrom(nodes);
        DiscoveryNode node = discoveryNodes.get(nodeId);
        DiscoveryNode newNode = new DiscoveryNode(node.getName(), node.getId(), node.getEphemeralId(), node.getHostName(),
                node.getHostAddress(), node.getAddress(), node.getAttributes(), node.getRoles(),
                randomVersionBetween(random(), Version.V_5_0_0, Version.V_5_4_0));

        return ClusterState.builder(clusterState).nodes(DiscoveryNodes.builder(discoveryNodes).remove(node).add(newNode)).build();

    }
}