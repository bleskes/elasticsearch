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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsRequest;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numClientNodes = 0, transportClientRatio = 0, numDataNodes = 3)
public class BootStrapTest extends AbstractAlertingTests {

    @Test
    public void testBootStrapAlerts() throws Exception {
        ensureGreen();

        SearchRequest searchRequest = createTriggerSearchRequest("my-index").source(searchSource().query(termQuery("field", "value")));
        BytesReference alertSource = createAlertSource("0 0/5 * * * ? *", searchRequest, "hits.total == 1");
        alertClient().prepareIndexAlert("my-first-alert")
                .setAlertSource(alertSource)
                .get();

        AlertsStatsRequest alertsStatsRequest = alertClient().prepareAlertsStats().request();
        AlertsStatsResponse response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(1L));

        refresh();

        String oldMaster = internalTestCluster().getMasterName();

        try {
            internalTestCluster().stopCurrentMasterNode();
        } catch (IOException ioe) {
            throw new ElasticsearchException("Failed to stop current master", ioe);
        }

        //Wait for alerts to start
        TimeValue maxTime = new TimeValue(30, TimeUnit.SECONDS);
        Thread.sleep(maxTime.getMillis());

        String newMaster = internalTestCluster().getMasterName();

        assertFalse(newMaster.equals(oldMaster));
        logger.info("Switched master from [{}] to [{}]",oldMaster,newMaster);

        alertsStatsRequest = alertClient().prepareAlertsStats().request();
        response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());

        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(1L));

    }

}
