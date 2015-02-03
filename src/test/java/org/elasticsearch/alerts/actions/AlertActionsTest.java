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

package org.elasticsearch.alerts.actions;


import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.alerts.*;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.support.AlertUtils;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertRequest;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertResponse;
import org.elasticsearch.alerts.transport.actions.get.GetAlertRequest;
import org.elasticsearch.alerts.transport.actions.get.GetAlertResponse;
import org.elasticsearch.alerts.transport.actions.put.PutAlertRequest;
import org.elasticsearch.alerts.transport.actions.put.PutAlertResponse;
import org.elasticsearch.alerts.triggers.AlertTrigger;
import org.elasticsearch.alerts.triggers.ScriptedTrigger;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.core.Is.is;

/**
 */
public class AlertActionsTest extends AbstractAlertingTests {

    @Test
    public void testAlertActionParser() throws Exception {
        DateTime fireTime = new DateTime(DateTimeZone.UTC);
        DateTime scheduledFireTime = new DateTime(DateTimeZone.UTC);

        Map<String, Object> triggerMap = new HashMap<>();
        Map<String, Object> scriptTriggerMap = new HashMap<>();
        scriptTriggerMap.put("script", "hits.total>1");
        scriptTriggerMap.put("script_lang", "groovy");
        triggerMap.put("script", scriptTriggerMap );


        Map<String,Object> actionMap = new HashMap<>();
        Map<String,Object> emailParamMap = new HashMap<>();
        List<String> addresses = new ArrayList<>();
        addresses.add("foo@bar.com");
        emailParamMap.put("addresses", addresses);
        actionMap.put("email", emailParamMap);

        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field(AlertActionService.ALERT_NAME_FIELD, "testName");
        builder.field(AlertActionService.TRIGGERED_FIELD, true);
        builder.field(AlertActionService.FIRE_TIME_FIELD, AlertUtils.dateTimeFormatter.printer().print(fireTime));
        builder.field(AlertActionService.SCHEDULED_FIRE_TIME_FIELD, AlertUtils.dateTimeFormatter.printer().print(scheduledFireTime));
        builder.field(AlertActionService.TRIGGER_FIELD, triggerMap);
        SearchRequest searchRequest = new SearchRequest("test123");
        builder.field(AlertActionService.TRIGGER_REQUEST);
        AlertUtils.writeSearchRequest(searchRequest, builder, ToXContent.EMPTY_PARAMS);
        SearchResponse searchResponse = new SearchResponse(
                new InternalSearchResponse(new InternalSearchHits(new InternalSearchHit[0], 10, 0), null, null, null, false, false),
                null, 1, 1, 0, new ShardSearchFailure[0]
        );
        builder.startObject(AlertActionService.TRIGGER_RESPONSE);
        builder.value(searchResponse);
        builder.endObject();
        builder.field(AlertActionService.ACTIONS_FIELD, actionMap);
        builder.field(AlertActionService.STATE, AlertActionState.SEARCH_NEEDED.toString());
        builder.endObject();
        final AlertActionRegistry alertActionRegistry = internalTestCluster().getInstance(AlertActionRegistry.class, internalTestCluster().getMasterName());
        final AlertActionService alertManager = internalTestCluster().getInstance(AlertActionService.class, internalTestCluster().getMasterName());

        AlertHistory actionEntry = alertManager.parseHistory("foobar", builder.bytes(), 0, alertActionRegistry);
        assertEquals(actionEntry.getVersion(), 0);
        assertEquals(actionEntry.getAlertName(), "testName");
        assertEquals(actionEntry.isTriggered(), true);
        assertEquals(actionEntry.getScheduledTime(), scheduledFireTime);
        assertEquals(actionEntry.getFireTime(), fireTime);
        assertEquals(actionEntry.getState(), AlertActionState.SEARCH_NEEDED);
        assertEquals(XContentMapValues.extractValue("hits.total", actionEntry.getTriggerResponse()), 10);
    }

    @Test
    public void testAlertActions() throws Exception {
        createIndex("my-index");

        ensureGreen("my-index");

        client().preparePutIndexedScript()
                .setScriptLang("mustache")
                .setId("query")
                .setSource(jsonBuilder().startObject().startObject("template").startObject("match_all").endObject().endObject().endObject())
                .get();

        final AlertsService alertsService = internalTestCluster().getInstance(AlertsService.class, internalTestCluster().getMasterName());
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(alertsService.getState(), is(AlertsService.State.STARTED));
            }
        });
        final AtomicBoolean alertActionInvoked = new AtomicBoolean(false);
        final AlertAction alertAction = new AlertAction() {
            @Override
            public String getActionName() {
                return "test";
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                builder.startObject();
                builder.endObject();
                return builder;
            }

        };
        AlertActionRegistry alertActionRegistry = internalTestCluster().getInstance(AlertActionRegistry.class, internalTestCluster().getMasterName());
        alertActionRegistry.registerAction("test", new AlertActionFactory() {
            @Override
            public AlertAction createAction(XContentParser parser) throws IOException {
                parser.nextToken();
                return alertAction;
            }

            @Override
            public boolean doAction(AlertAction action, Alert alert, TriggerResult actionEntry) {
                logger.info("Alert {} invoked: {}", alert.getAlertName(), actionEntry);
                alertActionInvoked.set(true);
                return true;
            }

        });

        AlertTrigger alertTrigger = new ScriptedTrigger("return true", ScriptService.ScriptType.INLINE, "groovy");


        Alert alert = new Alert(
                "my-first-alert",
                client().prepareSearch("my-index").setQuery(QueryBuilders.matchAllQuery()).request(),
                alertTrigger,
                Arrays.asList(alertAction),
                "0/5 * * * * ? *",
                null,
                1,
                new TimeValue(0),
                AlertAckState.NOT_ACKABLE
        );

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        alert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);

        AlertsClient alertsClient = internalTestCluster().getInstance(AlertsClient.class, internalTestCluster().getMasterName());

        PutAlertRequest alertRequest = alertsClient.preparePutAlert().setAlertName("my-first-alert").setAlertSource(jsonBuilder.bytes()).request();
        PutAlertResponse alertsResponse = alertsClient.putAlert(alertRequest).actionGet();
        assertNotNull(alertsResponse.indexResponse());
        assertTrue(alertsResponse.indexResponse().isCreated());

        GetAlertRequest getAlertRequest = new GetAlertRequest(alert.getAlertName());
        GetAlertResponse getAlertResponse = alertsClient.getAlert(getAlertRequest).actionGet();
        assertTrue(getAlertResponse.getResponse().isExists());
        assertEquals(getAlertResponse.getResponse().getSourceAsMap().get("schedule").toString(), "0/5 * * * * ? *");

        DeleteAlertRequest deleteAlertRequest = new DeleteAlertRequest(alert.getAlertName());
        DeleteAlertResponse deleteAlertResponse = alertsClient.deleteAlert(deleteAlertRequest).actionGet();
        assertNotNull(deleteAlertResponse.deleteResponse());
        assertTrue(deleteAlertResponse.deleteResponse().isFound());

        getAlertResponse = alertsClient.getAlert(getAlertRequest).actionGet();
        assertFalse(getAlertResponse.getResponse().isExists());

    }

}
