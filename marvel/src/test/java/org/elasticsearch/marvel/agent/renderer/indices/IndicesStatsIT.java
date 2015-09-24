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

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.marvel.agent.collector.indices.IndicesStatsCollector;
import org.elasticsearch.marvel.agent.renderer.AbstractRendererTestCase;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;

public class IndicesStatsIT extends AbstractRendererTestCase {

    @Override
    protected Collection<String> collectors() {
        return Collections.singletonList(IndicesStatsCollector.NAME);
    }

    @Test
    public void testIndicesStats() throws Exception {
        logger.debug("--> creating some indices for future indices stats");
        final int nbIndices = randomIntBetween(1, 5);
        for (int i = 0; i < nbIndices; i++) {
            createIndex("stat" + i);
        }

        final long[] nbDocsPerIndex = new long[nbIndices];
        for (int i = 0; i < nbIndices; i++) {
            nbDocsPerIndex[i] = randomIntBetween(1, 50);
            for (int j = 0; j < nbDocsPerIndex[i]; j++) {
                client().prepareIndex("stat" + i, "type1").setSource("num", i).get();
            }
        }

        awaitMarvelDocsCount(greaterThan(0L), IndicesStatsCollector.TYPE);

        logger.debug("--> wait for indicesx stats collector to collect global stat");
        assertBusy(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < nbIndices; i++) {
                    CountResponse count = client().prepareCount()
                            .setTypes(IndicesStatsCollector.TYPE)
                            .get();
                    assertThat(count.getCount(), greaterThan(0L));
                }
            }
        });

        logger.debug("--> searching for marvel documents of type [{}]", IndicesStatsCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(IndicesStatsCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = IndicesStatsRenderer.FILTERS;
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> indices stats successfully collected");
    }
}
