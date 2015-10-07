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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.marvel.agent.collector.shards.ShardsCollector;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.*;

@AwaitsFix(bugUrl="https://github.com/elastic/x-plugins/issues/729")
public class ShardsIT extends MarvelIntegTestCase {

    private static final String INDEX_PREFIX = "test-shards-";

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL, "3s")
                .put(MarvelSettings.COLLECTORS, ShardsCollector.NAME)
                .put(MarvelSettings.INDICES, INDEX_PREFIX + "*")
                .build();
    }

    @Test
    public void testShards() throws Exception {
        logger.debug("--> creating some indices so that shards collector reports data");
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            client().prepareIndex(INDEX_PREFIX + i, "foo").setRefresh(true).setSource("field1", "value1").get();
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

    /**
     * This test uses a terms aggregation to check that the "not_analyzed"
     * fields of the "shards" document type are indeed not analyzed
     */
    @Test
    public void testNotAnalyzedFields() throws Exception {
        final String indexName = INDEX_PREFIX + randomInt();
        assertAcked(prepareCreate(indexName).setSettings(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1, IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0));

        awaitMarvelDocsCount(greaterThan(0L), ShardsCollector.TYPE);

        SearchRequestBuilder searchRequestBuilder = client()
                .prepareSearch()
                .setTypes(ShardsCollector.TYPE)
                .setQuery(QueryBuilders.termQuery("shard.index", indexName));

        String[] notAnalyzedFields = {"state_uuid", "shard.state", "shard.index", "shard.node"};
        for (String field : notAnalyzedFields) {
            searchRequestBuilder.addAggregation(AggregationBuilders.terms("agg_" + field.replace('.', '_')).field(field));
        }

        SearchResponse response = searchRequestBuilder.get();
        assertThat(response.getHits().getTotalHits(), greaterThanOrEqualTo(1L));

        for (Aggregation aggregation : response.getAggregations()) {
            assertThat(aggregation, instanceOf(StringTerms.class));
            assertThat(((StringTerms) aggregation).getBuckets().size(), equalTo(1));
        }
    }
}
