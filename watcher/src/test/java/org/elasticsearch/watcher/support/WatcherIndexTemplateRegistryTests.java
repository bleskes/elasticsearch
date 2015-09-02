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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.watcher.WatcherModule;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.junit.Test;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

/**
 */
@ESIntegTestCase.ClusterScope(scope = TEST, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false, numDataNodes = 1)
public class WatcherIndexTemplateRegistryTests extends AbstractWatcherIntegrationTestCase {

    @Test
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
                GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates(WatcherModule.HISTORY_TEMPLATE_NAME).get();
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
                GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates(WatcherModule.HISTORY_TEMPLATE_NAME).get();
                assertThat(response.getIndexTemplates().size(), equalTo(1));
                // setting from the file on the classpath:
                assertThat(response.getIndexTemplates().get(0).getSettings().getAsBoolean("index.mapper.dynamic", null), is(false));
                // additional setting defined in the node settings:
                assertThat(response.getIndexTemplates().get(0).getSettings().get("index.key1"), equalTo("value"));
            }
        });
    }

}
