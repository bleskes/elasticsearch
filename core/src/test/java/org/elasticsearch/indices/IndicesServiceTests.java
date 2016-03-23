/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.indices;

import org.apache.lucene.store.LockObtainFailedException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.AliasAction;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.gateway.GatewayMetaState;
import org.elasticsearch.gateway.LocalAllocateDangledIndices;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardPath;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.test.IndexSettingsModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class IndicesServiceTests extends ESSingleNodeTestCase {

    public IndicesService getIndicesService() {
        return getInstanceFromNode(IndicesService.class);
    }

    public NodeEnvironment getNodeEnvironment() {
        return getInstanceFromNode(NodeEnvironment.class);
    }

    @Override
    protected boolean resetNodeAfterTest() {
        return true;
    }

    public void testCanDeleteIndexContent() throws IOException {
        final IndicesService indicesService = getIndicesService();
        final NodeEnvironment nodeEnv = getNodeEnvironment();
        final String indexName = "test";

        Settings settings = Settings.builder()
                                .put(IndexMetaData.SETTING_SHADOW_REPLICAS, true)
                                .put(IndexMetaData.SETTING_DATA_PATH, "/foo/bar")
                                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, randomIntBetween(1, 4))
                                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, randomIntBetween(0, 3))
                                .build();
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings(indexName, settings);
        Index index = idxSettings.getIndex();
        assertFalse("shard on shared filesystem", indicesService.canDeleteIndexContents(index, idxSettings, false));
        assertFalse("shard on shared filesystem and closed, but index doesn't exist",
            indicesService.canDeleteIndexContents(index, idxSettings, true));

        createIndexDirs(nodeEnv, index);
        // should be deletable since the index dirs now exist
        assertTrue("shard on shared filesystem and closed", indicesService.canDeleteIndexContents(index, idxSettings, true));
        deleteIndexDirs(nodeEnv, index);

        Settings.Builder newSettingsBuilder = Settings.builder().put(settings);
        newSettingsBuilder.remove(IndexMetaData.SETTING_SHADOW_REPLICAS);
        newSettingsBuilder.remove(IndexMetaData.SETTING_DATA_PATH);
        Settings newSettings = newSettingsBuilder.build();
        idxSettings = IndexSettingsModule.newIndexSettings(indexName, newSettings);
        index = idxSettings.getIndex();
        assertFalse("index not created, so nothing to delete", indicesService.canDeleteIndexContents(index, idxSettings, false));

        createIndex(indexName, newSettings);
        assertFalse("index created and active, so shouldn't be able to delete",
            indicesService.canDeleteIndexContents(index, idxSettings, false));
        indicesService.deleteIndex(index, "deleting test index");

        createIndexDirs(nodeEnv, index);
        assertTrue("index closed, should be able to delete",
            indicesService.canDeleteIndexContents(idxSettings.getIndex(), idxSettings, true));
        deleteIndexDirs(nodeEnv, index);
        assertFalse("no files for index, so should be nothing to delete",
            indicesService.canDeleteIndexContents(idxSettings.getIndex(), idxSettings, true));
    }

    public void testCanDeleteShardContent() {
        IndicesService indicesService = getIndicesService();
        IndexMetaData meta = IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(1).numberOfReplicas(
                1).build();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings("test", meta.getSettings());
        ShardId shardId = new ShardId(meta.getIndex(), 0);
        assertFalse("no shard location", indicesService.canDeleteShardContent(shardId, indexSettings));
        IndexService test = createIndex("test");
        shardId = new ShardId(test.index(), 0);
        assertTrue(test.hasShard(0));
        assertFalse("shard is allocated", indicesService.canDeleteShardContent(shardId, test.getIndexSettings()));
        test.removeShard(0, "boom");
        assertTrue("shard is removed", indicesService.canDeleteShardContent(shardId, test.getIndexSettings()));
        ShardId notAllocated = new ShardId(test.index(), 100);
        assertFalse("shard that was never on this node should NOT be deletable",
            indicesService.canDeleteShardContent(notAllocated, test.getIndexSettings()));
    }

    public void testDeleteIndexStore() throws Exception {
        IndicesService indicesService = getIndicesService();
        IndexService test = createIndex("test");
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        IndexMetaData firstMetaData = clusterService.state().metaData().index("test");
        assertTrue(test.hasShard(0));

        try {
            indicesService.deleteIndexStore("boom", firstMetaData, clusterService.state(), false);
            fail();
        } catch (IllegalStateException ex) {
            // all good
        }

        GatewayMetaState gwMetaState = getInstanceFromNode(GatewayMetaState.class);
        MetaData meta = gwMetaState.loadMetaState();
        assertNotNull(meta);
        assertNotNull(meta.index("test"));
        assertAcked(client().admin().indices().prepareDelete("test"));

        meta = gwMetaState.loadMetaState();
        assertNotNull(meta);
        assertNull(meta.index("test"));


        test = createIndex("test");
        client().prepareIndex("test", "type", "1").setSource("field", "value").setRefresh(true).get();
        client().admin().indices().prepareFlush("test").get();
        assertHitCount(client().prepareSearch("test").get(), 1);
        IndexMetaData secondMetaData = clusterService.state().metaData().index("test");
        assertAcked(client().admin().indices().prepareClose("test"));
        ShardPath path = ShardPath.loadShardPath(logger, getNodeEnvironment(), new ShardId(test.index(), 0), test.getIndexSettings());
        assertTrue(path.exists());

        try {
            indicesService.deleteIndexStore("boom", secondMetaData, clusterService.state(), false);
            fail();
        } catch (IllegalStateException ex) {
            // all good
        }

        assertTrue(path.exists());

        // now delete the old one and make sure we resolve against the name
        try {
            indicesService.deleteIndexStore("boom", firstMetaData, clusterService.state(), false);
            fail();
        } catch (IllegalStateException ex) {
            // all good
        }
        assertAcked(client().admin().indices().prepareOpen("test"));
        ensureGreen("test");
    }

    public void testPendingTasks() throws Exception {
        IndicesService indicesService = getIndicesService();
        IndexService test = createIndex("test");

        assertTrue(test.hasShard(0));
        ShardPath path = test.getShardOrNull(0).shardPath();
        assertTrue(test.getShardOrNull(0).routingEntry().started());
        ShardPath shardPath = ShardPath.loadShardPath(logger, getNodeEnvironment(), new ShardId(test.index(), 0), test.getIndexSettings());
        assertEquals(shardPath, path);
        try {
            indicesService.processPendingDeletes(test.index(), test.getIndexSettings(), new TimeValue(0, TimeUnit.MILLISECONDS));
            fail("can't get lock");
        } catch (LockObtainFailedException ex) {

        }
        assertTrue(path.exists());

        int numPending = 1;
        if (randomBoolean()) {
            indicesService.addPendingDelete(new ShardId(test.index(), 0), test.getIndexSettings());
        } else {
            if (randomBoolean()) {
                numPending++;
                indicesService.addPendingDelete(new ShardId(test.index(), 0), test.getIndexSettings());
            }
            indicesService.addPendingDelete(test.index(), test.getIndexSettings());
        }
        assertAcked(client().admin().indices().prepareClose("test"));
        assertTrue(path.exists());

        assertEquals(indicesService.numPendingDeletes(test.index()), numPending);

        // shard lock released... we can now delete
        indicesService.processPendingDeletes(test.index(), test.getIndexSettings(), new TimeValue(0, TimeUnit.MILLISECONDS));
        assertEquals(indicesService.numPendingDeletes(test.index()), 0);
        assertFalse(path.exists());

        if (randomBoolean()) {
            indicesService.addPendingDelete(new ShardId(test.index(), 0), test.getIndexSettings());
            indicesService.addPendingDelete(new ShardId(test.index(), 1), test.getIndexSettings());
            indicesService.addPendingDelete(new ShardId("bogus", "_na_", 1), test.getIndexSettings());
            assertEquals(indicesService.numPendingDeletes(test.index()), 2);
            // shard lock released... we can now delete
            indicesService.processPendingDeletes(test.index(), test.getIndexSettings(), new TimeValue(0, TimeUnit.MILLISECONDS));
            assertEquals(indicesService.numPendingDeletes(test.index()), 0);
        }
        assertAcked(client().admin().indices().prepareOpen("test"));

    }

    public void testDanglingIndicesWithAliasConflict() throws Exception {
        final String indexName = "test-idx1";
        final String alias = "test-alias";
        final IndicesService indicesService = getIndicesService();
        final ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        final IndexService test = createIndex(indexName);

        // create the alias for the index
        AliasAction action = new AliasAction(AliasAction.Type.ADD, indexName, alias);
        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(action);
        client().admin().indices().aliases(request).actionGet();
        final ClusterState originalState = clusterService.state();

        // try to import a dangling index with the same name as the alias, it should fail
        final LocalAllocateDangledIndices dangling = getInstanceFromNode(LocalAllocateDangledIndices.class);
        final Settings idxSettings = Settings.settingsBuilder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                                                               .put(IndexMetaData.SETTING_INDEX_UUID, Strings.randomBase64UUID())
                                                               .build();
        final IndexMetaData indexMetaData = new IndexMetaData.Builder(alias)
                                                             .settings(idxSettings)
                                                             .numberOfShards(1)
                                                             .numberOfReplicas(0)
                                                             .build();
        DanglingListener listener = new DanglingListener();
        dangling.allocateDangled(Arrays.asList(indexMetaData), listener);
        listener.latch.await();
        assertThat(clusterService.state(), equalTo(originalState));

        // remove the alias
        action = new AliasAction(AliasAction.Type.REMOVE, indexName, alias);
        request = new IndicesAliasesRequest().addAliasAction(action);
        client().admin().indices().aliases(request).actionGet();

        // now try importing a dangling index with the same name as the alias, it should succeed.
        listener = new DanglingListener();
        dangling.allocateDangled(Arrays.asList(indexMetaData), listener);
        listener.latch.await();
        assertThat(clusterService.state(), not(originalState));
        assertNotNull(clusterService.state().getMetaData().index(alias));

        // cleanup
        indicesService.deleteIndex(test.index(), "finished with test");
    }

    private static class DanglingListener implements LocalAllocateDangledIndices.Listener {
        final CountDownLatch latch = new CountDownLatch(1);

        DanglingListener() { }

        @Override
        public void onResponse(LocalAllocateDangledIndices.AllocateDangledResponse response) {
            latch.countDown();
        }

        @Override
        public void onFailure(Throwable e) {
            latch.countDown();
        }
    }

    // create the dirs for an index to simulate the index existing on the file system
    private static void createIndexDirs(final NodeEnvironment nodeEnv, final Index index) throws IOException {
        for (Path dir : nodeEnv.indexPaths(index)) {
            if (Files.exists(dir) == false) {
                Files.createDirectories(dir);
            }
        }
    }

    // returns true iff all index paths for the index were successfully deleted
    private static boolean deleteIndexDirs(final NodeEnvironment nodeEnv, final Index index) throws IOException {
        boolean success = true;
        for (Path dir : nodeEnv.indexPaths(index)) {
            FileSystemUtils.deleteAllFiles(dir);
            if (Files.deleteIfExists(dir) == false) {
                success = false;
            }
        }
        return success;
    }
}
