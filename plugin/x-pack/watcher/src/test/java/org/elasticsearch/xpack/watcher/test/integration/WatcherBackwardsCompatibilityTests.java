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
package org.elasticsearch.xpack.watcher.test.integration;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.MockScriptPlugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.watcher.support.xcontent.ObjectPath;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.xpack.watcher.watch.WatchStore;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0, numClientNodes = 0, transportClientRatio = 0)
@LuceneTestCase.SuppressFileSystems("ExtrasFS")
public class WatcherBackwardsCompatibilityTests extends AbstractWatcherIntegrationTestCase {

    private static final String INDEX_NAME = WatchStore.INDEX;
    private static final String TYPE_NAME = WatchStore.DOC_TYPE;

    @Override
    protected boolean enableSecurity() {
        return false;
    }

    @Override
    protected boolean timeWarped() {
        return false;
    }

    @Override
    protected List<Class<? extends Plugin>> pluginTypes() {
        List<Class<? extends Plugin>> plugins = super.pluginTypes();
        plugins.add(FoolMeScriptLang.class);
        return plugins;
    }

    public void testWatchLoadedSuccessfullyAfterUpgrade() throws Exception {
        // setup node
        Path repoDir = createTempDir();
        try (InputStream stream = WatcherBackwardsCompatibilityTests.class.
                getResourceAsStream("/bwc_indices/bwc_watcher_index_snapshot_repo_2.3.5.zip")) {
            TestUtil.unzip(stream, repoDir);
        }

        Settings.Builder nodeSettings = Settings.builder()
                .put(super.nodeSettings(0))
                .put(Environment.PATH_REPO_SETTING.getKey(), repoDir);
        internalCluster().startNode(nodeSettings.build());
        ensureYellow();

        // stop watcher to be able to trigger the restore
        assertAcked(watcherClient().prepareWatchService().stop().get());

        // create repo to back up from
        client().admin().cluster().preparePutRepository("backup")
                .setType("fs")
                .setSettings(Settings.builder().put("location", repoDir).put("compress", true))
                .get();

        // restore the index
        // POST /_snapshot/backup/snapshot/_restore
        RestoreSnapshotResponse response = client().admin().cluster()
                .prepareRestoreSnapshot("backup", "snapshot")
                .setWaitForCompletion(true)
                .get();
        assertThat(response.status(), is(RestStatus.OK));

        // lets get up and running again
        assertAcked(watcherClient().prepareWatchService().start().get());

        // verify cluster state:
        assertBusy(() -> {
            ClusterState state = client().admin().cluster().prepareState().get().getState();
            assertThat(state.metaData().indices().size(), equalTo(1)); // only the .watches index
            // (the watch has a very high interval (99 weeks))
            assertThat(state.metaData().indices().get(INDEX_NAME), notNullValue());
            assertThat(state.metaData().indices().get(INDEX_NAME).getCreationVersion(), equalTo(Version.V_2_3_5));
            assertThat(state.metaData().indices().get(INDEX_NAME).getUpgradedVersion(), equalTo(Version.CURRENT));
            assertThat(state.metaData().indices().get(INDEX_NAME).getMappings().size(), equalTo(1));
            assertThat(state.metaData().indices().get(INDEX_NAME).getMappings().get(TYPE_NAME), notNullValue());
        });

        // verify existing watcher documents:
        SearchResponse searchResponse = client().prepareSearch(INDEX_NAME)
            .setTypes(TYPE_NAME)
            .get();
        assertThat(searchResponse.getHits().getTotalHits(), equalTo(1L));
        assertThat(searchResponse.getHits().getAt(0).id(), equalTo("log_error_watch"));

        // Verify that we can get the watch, which means the watch stored in ES 2.3.5 cluster has been successfully
        // loaded with the current version of ES:
        ensureWatcherStarted();
        assertThat(watcherClient().prepareWatcherStats().get().getWatchesCount(), equalTo(1L));
        GetWatchResponse getWatchResponse = watcherClient().prepareGetWatch("log_error_watch").get();
        assertThat(getWatchResponse.isFound(), is(true));
        Map<String, Object> watchSourceAsMap = getWatchResponse.getSource().getAsMap();
        assertThat(ObjectPath.eval("trigger.schedule.interval", watchSourceAsMap), equalTo("99w"));
        assertThat(ObjectPath.eval("input.search.request.body.query.bool.filter.1.range.date.to", watchSourceAsMap),
                equalTo("{{ctx.trigger.scheduled_time}}"));
        assertThat(ObjectPath.eval("actions.log_error.logging.text", watchSourceAsMap),
                equalTo("Found {{ctx.payload.hits.total}} errors in the logs"));

        // Check that all scripts have been upgraded, so that the language has been set to groovy (legacy language default):
        assertThat(ObjectPath.eval("input.search.request.body.query.bool.filter.2.script.script.lang", watchSourceAsMap),
                equalTo("groovy"));
        assertThat(ObjectPath.eval("input.search.request.body.aggregations.avg_grade.avg.script.lang", watchSourceAsMap),
                equalTo("groovy"));
        assertThat(ObjectPath.eval("condition.script.lang", watchSourceAsMap), equalTo("groovy"));
    }

    // Fool the script service that this is the groovy script language, so that we can just load the watch upon startup
    // and verify that the lang options on scripts have been set.
    public static class FoolMeScriptLang extends MockScriptPlugin{

        @Override
        protected Map<String, Function<Map<String, Object>, Object>> pluginScripts() {
            return Collections.singletonMap("ctx.payload.hits.total > 0", (vars) -> true);
        }

        @Override
        public String pluginScriptLang() {
            return "groovy";
        }
    }

}
