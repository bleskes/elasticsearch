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

package org.elasticsearch.alerts.history;

import org.elasticsearch.alerts.*;
import org.elasticsearch.alerts.actions.email.EmailAction;
import org.elasticsearch.alerts.actions.webhook.WebhookAction;
import org.elasticsearch.alerts.throttle.Throttler;
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.alerts.trigger.search.ScriptSearchTrigger;
import org.elasticsearch.alerts.trigger.search.SearchTrigger;
import org.elasticsearch.alerts.trigger.simple.SimpleTrigger;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;

/**
 */
public class FiredAlertTest extends AbstractAlertingTests {

    @Test
    public void testFiredAlertParser() throws Exception {

        Alert alert = createTestAlert("fired_test");
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);


        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertEquals(jsonBuilder.bytes().toUtf8(), jsonBuilder2.bytes().toUtf8());

    }

    @Test
    public void testFinalizedFiredAlertParser() throws Exception {
        Alert alert = createTestAlert("fired_test");
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        ExecutionContext ctx = new ExecutionContext(firedAlert.id(), alert, new DateTime(), new DateTime());
        ctx.onActionResult(new EmailAction.Result.Failure("failed to send because blah"));
        ctx.onActionResult(new WebhookAction.Result.Executed(300, "http://localhost:8000/alertfoo", "{'awesome' : 'us'}"));
        Trigger.Result triggerResult = new SimpleTrigger.Result(new Payload.Simple());
        ctx.onThrottleResult(Throttler.NO_THROTTLE.throttle(ctx, triggerResult));
        ctx.onTriggerResult(triggerResult);
        firedAlert.update(new AlertExecution(ctx));
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        logger.error("FOO : " + jsonBuilder.bytes().toUtf8());
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);
        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);
        assertEquals(jsonBuilder.bytes().toUtf8(), jsonBuilder2.bytes().toUtf8());
    }

    @Test
    public void testFinalizedFiredAlertParserScriptSearchTrigger() throws Exception {
        Alert alert = createTestAlert("fired_test");
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        ExecutionContext ctx = new ExecutionContext(firedAlert.id(), alert, new DateTime(), new DateTime());
        ctx.onActionResult(new EmailAction.Result.Failure("failed to send because blah"));
        ctx.onActionResult(new WebhookAction.Result.Executed(300, "http://localhost:8000/alertfoo", "{'awesome' : 'us'}"));
        Trigger.Result triggerResult = new SearchTrigger.Result(ScriptSearchTrigger.TYPE, true, createTriggerSearchRequest(), new Payload.Simple());
        ctx.onThrottleResult(Throttler.NO_THROTTLE.throttle(ctx, triggerResult));
        ctx.onTriggerResult(triggerResult);
        firedAlert.update(new AlertExecution(ctx));
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);
        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);
        assertEquals(jsonBuilder.bytes().toUtf8(), jsonBuilder2.bytes().toUtf8());
    }


}
