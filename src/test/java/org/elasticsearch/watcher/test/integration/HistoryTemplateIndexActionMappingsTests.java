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

package org.elasticsearch.watcher.test.integration;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.watcher.execution.ExecutionState;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.junit.Test;

import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test makes sure that the index action response `index` and `type` fields in the watch_record action result are
 * not analyzed so they can be used in aggregations
 */
public class HistoryTemplateIndexActionMappingsTests extends AbstractWatcherIntegrationTests {

    @Override
    protected boolean timeWarped() {
        return true; // just to have better control over the triggers
    }

    @Override
    protected boolean enableShield() {
        return false; // remove shield noise from this test
    }

    @Test
    public void testIndexActionFields() throws Exception {
        String index = "the-index";
        String type = "the-type";

        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(interval("5s")))
                .addAction("index", indexAction(index, type)))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));
        timeWarp().scheduler().trigger("_id");
        flush();
        refresh();

        // the action should fail as no email server is available
        assertWatchWithMinimumActionsCount("_id", ExecutionState.EXECUTED, 1);
        flush();
        refresh();

        SearchResponse response = client().prepareSearch(HistoryStore.INDEX_PREFIX + "*").setSource(searchSource()
                .aggregation(terms("index_action_indices").field("execution_result.actions.index.response.index"))
                .aggregation(terms("index_action_types").field("execution_result.actions.index.response.type"))
                .buildAsBytes())
                .get();

        assertThat(response, notNullValue());
        assertThat(response.getHits().getTotalHits(), is(1L));
        Aggregations aggs = response.getAggregations();
        assertThat(aggs, notNullValue());

        Terms terms = aggs.get("index_action_indices");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(1));
        assertThat(terms.getBucketByKey(index), notNullValue());
        assertThat(terms.getBucketByKey(index).getDocCount(), is(1L));

        terms = aggs.get("index_action_types");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(1));
        assertThat(terms.getBucketByKey(type), notNullValue());
        assertThat(terms.getBucketByKey(type).getDocCount(), is(1L));
    }
}
