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
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.junit.Test;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.SUITE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = SUITE, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false, numDataNodes = 1)
public class TemplateUtilsTests extends ElasticsearchIntegrationTest {

    @Test
    public void testPutTemplate() throws Exception {
        TemplateUtils templateUtils = new TemplateUtils(Settings.EMPTY, ClientProxy.of(client()));

        Settings.Builder options = Settings.builder();
        options.put("key", "value");
        templateUtils.putTemplate(HistoryStore.INDEX_TEMPLATE_NAME, options.build());

        GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates(HistoryStore.INDEX_TEMPLATE_NAME).get();
        // setting in the file on the classpath:
        assertThat(response.getIndexTemplates().get(0).getSettings().getAsBoolean("index.mapper.dynamic", null), is(false));
        // additional setting:
        assertThat(response.getIndexTemplates().get(0).getSettings().get("index.key"), equalTo("value"));
    }

}
