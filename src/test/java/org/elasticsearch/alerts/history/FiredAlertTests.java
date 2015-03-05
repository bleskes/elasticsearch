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

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.AlertExecution;
import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.actions.email.EmailAction;
import org.elasticsearch.alerts.actions.webhook.WebhookAction;
import org.elasticsearch.alerts.condition.Condition;
import org.elasticsearch.alerts.condition.simple.AlwaysFalseCondition;
import org.elasticsearch.alerts.condition.simple.AlwaysTrueCondition;
import org.elasticsearch.alerts.input.Input;
import org.elasticsearch.alerts.input.simple.SimpleInput;
import org.elasticsearch.alerts.test.AbstractAlertsIntegrationTests;
import org.elasticsearch.alerts.test.AlertsTestUtils;
import org.elasticsearch.alerts.throttle.Throttler;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 */
public class FiredAlertTests extends AbstractAlertsIntegrationTests {

    @Test
    public void testParser() throws Exception {
        Alert alert = AlertsTestUtils.createTestAlert("fired_test", scriptService(), httpClient(), noopEmailService(), logger);
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }

    @Test
    public void testParser_WithSealedFiredAlert() throws Exception {
        Alert alert = AlertsTestUtils.createTestAlert("fired_test", scriptService(), httpClient(), noopEmailService(), logger);
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        ExecutionContext ctx = new ExecutionContext(firedAlert.id(), alert, new DateTime(), new DateTime(), new DateTime());
        ctx.onActionResult(new EmailAction.Result.Failure("failed to send because blah"));
        ctx.onActionResult(new WebhookAction.Result.Executed(300, "http://localhost:8000/alertfoo", "{'awesome' : 'us'}"));
        Input.Result inputResult = new SimpleInput.Result(SimpleInput.TYPE, new Payload.Simple());
        Condition.Result conditionResult = AlwaysTrueCondition.RESULT;
        ctx.onThrottleResult(Throttler.NO_THROTTLE.throttle(ctx));
        ctx.onInputResult(inputResult);
        ctx.onConditionResult(conditionResult);
        firedAlert.update(new AlertExecution(ctx));

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }

    @Test
    public void testParser_WithSealedFiredAlert_WithScriptSearchCondition() throws Exception {
        Alert alert = AlertsTestUtils.createTestAlert("fired_test", scriptService(), httpClient(), noopEmailService(), logger);
        FiredAlert firedAlert = new FiredAlert(alert, new DateTime(), new DateTime());
        ExecutionContext ctx = new ExecutionContext(firedAlert.id(), alert, new DateTime(), new DateTime(), new DateTime());
        ctx.onActionResult(new EmailAction.Result.Failure("failed to send because blah"));
        ctx.onActionResult(new WebhookAction.Result.Executed(300, "http://localhost:8000/alertfoo", "{'awesome' : 'us'}"));
        Input.Result inputResult = new SimpleInput.Result(SimpleInput.TYPE, new Payload.Simple());
        Condition.Result conditionResult = AlwaysFalseCondition.RESULT;
        ctx.onThrottleResult(Throttler.NO_THROTTLE.throttle(ctx));
        ctx.onInputResult(inputResult);
        ctx.onConditionResult(conditionResult);
        firedAlert.update(new AlertExecution(ctx));

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        firedAlert.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        FiredAlert parsedFiredAlert = firedAlertParser().parse(jsonBuilder.bytes(), firedAlert.id(), 0);

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedFiredAlert.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }


}
