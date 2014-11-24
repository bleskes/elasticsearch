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


package org.elasticsearch.alerts;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.actions.AlertAction;
import org.elasticsearch.alerts.actions.AlertActionEntry;
import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.alerts.actions.AlertActionState;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.alerts.triggers.ScriptedTrigger;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.script.ScriptService;
import org.junit.Test;

import java.util.ArrayList;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 */
public class BootStrapTest extends AbstractAlertingTests {

    @Test
    public void testBootStrapAlerts() throws Exception {
        ensureAlertingStarted();

        SearchRequest searchRequest = createTriggerSearchRequest("my-index").source(searchSource().query(termQuery("field", "value")));
        BytesReference alertSource = createAlertSource("0 0/5 * * * ? *", searchRequest, "hits.total == 1");
        client().prepareIndex(AlertsStore.ALERT_INDEX, AlertsStore.ALERT_TYPE, "my-first-alert")
                .setSource(alertSource)
                .setConsistencyLevel(WriteConsistencyLevel.ALL)
                .get();

        client().admin().indices().prepareRefresh(AlertsStore.ALERT_INDEX).get();
        stopAlerting();
        startAlerting();

        AlertsStatsResponse response = alertClient().prepareAlertsStats().get();
        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(1L));
    }

    @Test
    public void testBootStrapHistory() throws Exception {
        ensureAlertingStarted();
        internalTestCluster().ensureAtLeastNumDataNodes(2);

        AlertsStatsResponse response = alertClient().prepareAlertsStats().get();
        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(0L));

        SearchRequest searchRequest = createTriggerSearchRequest("my-index").source(searchSource().query(termQuery("field", "value")));
        Alert alert = new Alert("my-first-alert",
                searchRequest,
                new ScriptedTrigger("hits.total == 1", ScriptService.ScriptType.INLINE, "groovy"),
                new ArrayList< AlertAction>(),
                "0 0/5 * * * ? *",
                new DateTime(),
                0,
                true,
                new TimeValue(0),
                AlertAckState.NOT_ACKABLE);

        AlertActionEntry entry = new AlertActionEntry(alert, new DateTime(), new DateTime(), AlertActionState.SEARCH_NEEDED);
        IndexResponse indexResponse = client().prepareIndex(AlertActionManager.ALERT_HISTORY_INDEX, AlertActionManager.ALERT_HISTORY_TYPE, entry.getId())
                .setConsistencyLevel(WriteConsistencyLevel.ALL)
                .setSource(XContentFactory.jsonBuilder().value(entry))
                .get();
        assertTrue(indexResponse.isCreated());

        stopAlerting();
        startAlerting();

        response = alertClient().prepareAlertsStats().get();
        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(0L));
        assertThat(response.getAlertActionManagerLargestQueueSize(), equalTo(1L));
    }

}
