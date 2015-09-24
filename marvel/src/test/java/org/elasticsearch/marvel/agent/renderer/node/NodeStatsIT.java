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

package org.elasticsearch.marvel.agent.renderer.node;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.marvel.agent.collector.node.NodeStatsCollector;
import org.elasticsearch.marvel.agent.renderer.AbstractRendererTestCase;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;

public class NodeStatsIT extends AbstractRendererTestCase {

    @Override
    protected Collection<String> collectors() {
        return Collections.singletonList(NodeStatsCollector.NAME);
    }

    @Test
    public void testNodeStats() throws Exception {
        logger.debug("--> creating some indices for future node stats");
        final int numDocs = between(50, 150);
        for (int i = 0; i < numDocs; i++) {
            client().prepareIndex("test", "foo").setSource("value", randomInt()).get();
        }

        awaitMarvelDocsCount(greaterThan(0L), NodeStatsCollector.TYPE);

        logger.debug("--> searching for marvel documents of type [{}]", NodeStatsCollector.TYPE);
        SearchResponse response = client().prepareSearch().setTypes(NodeStatsCollector.TYPE).get();
        assertThat(response.getHits().getTotalHits(), greaterThan(0L));

        logger.debug("--> checking that every document contains the expected fields");
        String[] filters = NodeStatsRenderer.FILTERS;
        for (SearchHit searchHit : response.getHits().getHits()) {
            Map<String, Object> fields = searchHit.sourceAsMap();

            for (String filter : filters) {
                assertContains(filter, fields);
            }
        }

        logger.debug("--> node stats successfully collected");
    }
}
