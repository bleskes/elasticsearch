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

package org.elasticsearch.marvel.agent.renderer.indices;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.indices.IndexRecoveryCollector;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;

public class IndexRecoveryIT extends MarvelIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL, "3s")
                .put(MarvelSettings.COLLECTORS, IndexRecoveryCollector.NAME)
                .build();
    }

    @Test
    public void testIndexRecovery() throws Exception {
        logger.debug("--> creating some indices so that index recovery collector reports data");
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            client().prepareIndex("test-" + i, "foo").setRefresh(true).setSource("field1", "value1").get();
        }

        awaitMarvelDocsCount(greaterThan(0L), IndexRecoveryCollector.TYPE);

        logger.debug("--> searching for marvel documents of type [{}]", IndexRecoveryCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(IndexRecoveryCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = {
                IndexRecoveryRenderer.Fields.INDEX_RECOVERY.underscore().toString(),
                IndexRecoveryRenderer.Fields.INDEX_RECOVERY.underscore().toString() + "." + IndexRecoveryRenderer.Fields.SHARDS.underscore().toString(),
        };

        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> index recovery successfully collected");
    }
}
