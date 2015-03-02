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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.*;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.test.AbstractAlertsIntegrationTests;
import org.elasticsearch.alerts.test.AlertsTestUtils;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsRequest;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;


/**
 */
@ClusterScope(scope = TEST, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false)
public class AlertStatsTests extends AbstractAlertsIntegrationTests {

    @Test
    public void testStartedStats() throws Exception {
        AlertsStatsRequest alertsStatsRequest = alertClient().prepareAlertsStats().request();
        AlertsStatsResponse response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        assertThat(response.isAlertActionManagerStarted(), is(true));
        assertThat(response.getAlertManagerStarted(), is(AlertsService.State.STARTED));
        assertThat(response.getAlertActionManagerQueueSize(), is(0L));
        assertThat(response.getNumberOfRegisteredAlerts(), is(0L));
        assertThat(response.getAlertActionManagerLargestQueueSize(), is(0L));
        assertThat(response.getVersion(), is(AlertsVersion.CURRENT));
        assertThat(response.getBuild(), is(AlertsBuild.CURRENT));
    }

    @Test
    public void testAlertCountStats() throws Exception {
        AlertsClient alertsClient = alertClient();

        AlertsStatsRequest alertsStatsRequest = alertsClient.prepareAlertsStats().request();
        AlertsStatsResponse response = alertsClient.alertsStats(alertsStatsRequest).actionGet();

        assertThat(response.isAlertActionManagerStarted(), is(true));
        assertThat(response.getAlertManagerStarted(), equalTo(AlertsService.State.STARTED));

        SearchRequest searchRequest = AlertsTestUtils.newInputSearchRequest("my-index").source(searchSource().query(termQuery("field", "value")));
        BytesReference alertSource = createAlertSource("* * * * * ? *", searchRequest, "payload.hits.total == 1");
        alertClient().preparePutAlert("testAlert")
                .source(alertSource)
                .get();

        response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        //Wait a little until we should have queued an action
        TimeValue waitTime = new TimeValue(30, TimeUnit.SECONDS);
        Thread.sleep(waitTime.getMillis());

        assertThat(response.isAlertActionManagerStarted(), is(true));
        assertThat(response.getAlertManagerStarted(), is(AlertsService.State.STARTED));
        assertThat(response.getNumberOfRegisteredAlerts(), is(1L));
        //assertThat(response.getAlertActionManagerLargestQueueSize(), greaterThan(0L));
    }
}
