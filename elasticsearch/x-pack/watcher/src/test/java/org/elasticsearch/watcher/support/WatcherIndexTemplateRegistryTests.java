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

package org.elasticsearch.watcher.support;

import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.watcher.WatcherModule;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

/**
 */
@ESIntegTestCase.ClusterScope(scope = TEST, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false, numDataNodes = 1)
public class WatcherIndexTemplateRegistryTests extends AbstractWatcherIntegrationTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        ArrayList<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(SettingTestPlugin.class);
        return plugins;
    }

    public void testTemplates() throws Exception {
        assertAcked(
                client().admin().cluster().prepareUpdateSettings()
                        .setTransientSettings(Settings.builder()
                                .put("watcher.history.index.key1", "value"))
                        .get()
        );

        assertBusy(new Runnable() {
            @Override
            public void run() {
                GetIndexTemplatesResponse response = client().admin().indices()
                        .prepareGetTemplates(WatcherModule.HISTORY_TEMPLATE_NAME).get();
                assertThat(response.getIndexTemplates().size(), equalTo(1));
                // setting from the file on the classpath:
                assertThat(response.getIndexTemplates().get(0).getSettings().getAsBoolean("index.mapper.dynamic", null), is(false));
                // additional setting defined in the node settings:
                assertThat(response.getIndexTemplates().get(0).getSettings().get("index.key1"), equalTo("value"));
            }
        });

        // Now delete the index template and verify the index template gets added back:
        assertAcked(client().admin().indices().prepareDeleteTemplate(WatcherModule.HISTORY_TEMPLATE_NAME).get());

        assertBusy(new Runnable() {
            @Override
            public void run() {
                GetIndexTemplatesResponse response = client().admin().indices()
                        .prepareGetTemplates(WatcherModule.HISTORY_TEMPLATE_NAME).get();
                assertThat(response.getIndexTemplates().size(), equalTo(1));
                // setting from the file on the classpath:
                assertThat(response.getIndexTemplates().get(0).getSettings().getAsBoolean("index.mapper.dynamic", null), is(false));
                // additional setting defined in the node settings:
                assertThat(response.getIndexTemplates().get(0).getSettings().get("index.key1"), equalTo("value"));
            }
        });
    }

    public static class SettingTestPlugin extends Plugin {
        public String name() {
            return "watcher-index-template-registry-settings-plugin";
        }

        @Override
        public String description() {
            return "installs a setting this test needs";
        }

        public static final Setting<String> KEY_1 = new Setting<>("index.key1", "", Function.identity(), Setting.Property.IndexScope);

        public void onModule(SettingsModule module) {
            module.registerSetting(KEY_1);
        }
    }
}
