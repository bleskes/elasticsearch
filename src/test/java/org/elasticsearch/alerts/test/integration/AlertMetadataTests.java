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

package org.elasticsearch.alerts.test.integration;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.history.HistoryStore;
import org.elasticsearch.alerts.test.AbstractAlertsIntegrationTests;
import org.elasticsearch.alerts.test.AlertsTestUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.alerts.client.AlertSourceBuilder.alertSourceBuilder;
import static org.elasticsearch.alerts.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.alerts.input.InputBuilders.searchInput;
import static org.elasticsearch.alerts.scheduler.schedule.Schedules.cron;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.Matchers.greaterThan;

/**
 *
 */
public class AlertMetadataTests extends AbstractAlertsIntegrationTests {

    @Test
    public void testAlertMetadata() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", "bar");
        List<String> metaList = new ArrayList<>();
        metaList.add("this");
        metaList.add("is");
        metaList.add("a");
        metaList.add("test");

        metadata.put("baz", metaList);
        alertClient().preparePutAlert("1")
                .source(alertSourceBuilder()
                        .schedule(cron("0/5 * * * * ? *"))
                        .input(searchInput(AlertsTestUtils.newInputSearchRequest("my-index").source(searchSource().query(matchAllQuery()))))
                        .condition(scriptCondition("ctx.payload.hits.total == 1"))
                        .metadata(metadata))
                .get();
        // Wait for a no action entry to be added. (the condition search request will not match, because there are no docs in my-index)
        assertAlertWithNoActionNeeded("1", 1);

        refresh();
        SearchResponse searchResponse = client().prepareSearch(HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*")
                .setQuery(termQuery("meta.foo", "bar"))
                .get();
        assertThat(searchResponse.getHits().getTotalHits(), greaterThan(0L));
    }

}
