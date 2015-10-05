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

package org.elasticsearch.marvel.agent.renderer.shards;

import org.apache.lucene.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.shards.ShardsCollector;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;

@AwaitsFix(bugUrl="https://github.com/elastic/x-plugins/issues/729")
public class ShardsIT extends MarvelIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL, "3s")
                .put(MarvelSettings.COLLECTORS, ShardsCollector.NAME)
                .build();
    }

    @Test
    public void testShards() throws Exception {
        logger.debug("--> creating some indices so that shards collector reports data");
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            client().prepareIndex("test-" + i, "foo").setRefresh(true).setSource("field1", "value1").get();
        }

        awaitMarvelDocsCount(greaterThan(0L), ShardsCollector.TYPE);

        logger.debug("--> searching for marvel documents of type [{}]", ShardsCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(ShardsCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = ShardsRenderer.FILTERS;
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> shards successfully collected");
    }
}
