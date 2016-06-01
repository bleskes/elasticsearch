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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xpack.watcher.actions.ActionBuilders;
import org.elasticsearch.xpack.watcher.client.WatchSourceBuilder;
import org.elasticsearch.xpack.watcher.client.WatchSourceBuilders;
import org.elasticsearch.xpack.watcher.input.InputBuilders;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.xpack.watcher.trigger.TriggerBuilders;
import org.elasticsearch.xpack.watcher.trigger.schedule.Schedules;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.is;

public class HistoryIntegrationTests extends AbstractWatcherIntegrationTestCase {

    // issue: https://github.com/elastic/x-plugins/issues/2338
    public void testThatHistoryIsWrittenWithChainedInput() throws Exception {
        XContentBuilder xContentBuilder = jsonBuilder().startObject().startObject("inner").field("date", "2015-06-06").endObject()
                .endObject();
        index("foo", "bar", "1", xContentBuilder);
        refresh();

        WatchSourceBuilder builder = WatchSourceBuilders.watchBuilder()
                .trigger(TriggerBuilders.schedule(Schedules.interval("10s")))
                .addAction("logging", ActionBuilders.loggingAction("foo"));

        SearchRequestBuilder searchRequestBuilder = client().prepareSearch("foo").addSort(SortBuilders.fieldSort("inner.date").order
                (SortOrder.DESC));
        builder.input(InputBuilders.chainInput().add("first", InputBuilders.searchInput(searchRequestBuilder)));

        PutWatchResponse response = watcherClient().preparePutWatch("test_watch").setSource(builder).get();
        assertThat(response.isCreated(), is(true));

        watcherClient().prepareExecuteWatch("test_watch").setRecordExecution(true).get();

        flushAndRefresh(".watcher-history-*");
        SearchResponse searchResponse = client().prepareSearch(".watcher-history-*").get();
        assertHitCount(searchResponse, 1);
    }
}
