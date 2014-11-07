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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.actions.AlertAction;
import org.elasticsearch.alerts.triggers.ScriptedTrigger;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import java.util.ArrayList;

public class TestAlertSerialization extends ElasticsearchIntegrationTest {

    @Test
    public void testAlertSerialization() throws Exception {
        Alert alert = new Alert("test-serialization",
                new SearchRequest(),
                new ScriptedTrigger("return true", null, null),
                new ArrayList<AlertAction>(), "0/5 * * * * ? *",
                new DateTime(),
                0,
                false);

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        alert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);

        final AlertsStore alertsStore =
                internalCluster().getInstance(AlertsStore.class, internalCluster().getMasterName());

        Alert parsedAlert = alertsStore.parseAlert("test-serialization", jsonBuilder.bytes());
        assertEquals(parsedAlert, alert);
    }

}
