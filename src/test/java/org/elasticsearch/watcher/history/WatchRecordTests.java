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

package org.elasticsearch.watcher.history;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.actions.email.EmailAction;
import org.elasticsearch.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.watcher.execution.TriggeredExecutionContext;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.execution.WatchExecutionResult;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.support.http.HttpRequest;
import org.elasticsearch.watcher.support.http.HttpResponse;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.watch.Watch;
import org.junit.Test;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
import static org.elasticsearch.common.unit.TimeValue.timeValueSeconds;
import static org.hamcrest.Matchers.equalTo;

/**
 */
public class WatchRecordTests extends AbstractWatcherIntegrationTests {

    @Test
    public void testParser() throws Exception {
        Watch watch = WatcherTestUtils.createTestWatch("fired_test", scriptService(), watcherHttpClient(), noopEmailService(), logger);
        ScheduleTriggerEvent event = new ScheduleTriggerEvent(watch.id(), DateTime.now(UTC), DateTime.now(UTC));
        Wid wid = new Wid("_record", randomLong(), DateTime.now(UTC));
        WatchRecord watchRecord = new WatchRecord(wid, watch, event);
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        watchRecord.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        WatchRecord parsedWatchRecord = watchRecordParser().parse(watchRecord.id().value(), 0, jsonBuilder.bytes());

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedWatchRecord.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }

    @Test
    public void testParser_WithSealedWatchRecord() throws Exception {
        Watch watch = WatcherTestUtils.createTestWatch("fired_test", scriptService(), watcherHttpClient(), noopEmailService(), logger);
        ScheduleTriggerEvent event = new ScheduleTriggerEvent(watch.id(), DateTime.now(UTC), DateTime.now(UTC));
        Wid wid = new Wid("_record", randomLong(), DateTime.now(UTC));
        WatchRecord watchRecord = new WatchRecord(wid, watch, event);
        WatchExecutionContext ctx = new TriggeredExecutionContext(watch, new DateTime(), event, timeValueSeconds(5));
        ctx.onActionResult(new ActionWrapper.Result("_email", new Action.Result.Failure(EmailAction.TYPE, "failed to send because blah")));
        HttpRequest request = HttpRequest.builder("localhost", 8000)
                .path("/watchfoo")
                .body("{'awesome' : 'us'}")
                .build();
        ctx.onActionResult(new ActionWrapper.Result("_webhook", new WebhookAction.Result.Success(request, new HttpResponse(300))));
        SimpleInput.Result inputResult = new SimpleInput.Result(new Payload.Simple());
        Condition.Result conditionResult = AlwaysCondition.Result.INSTANCE;
        ctx.onInputResult(inputResult);
        ctx.onConditionResult(conditionResult);
        long watchExecutionDuration = randomIntBetween(30, 100000);
        watchRecord.seal(new WatchExecutionResult(ctx, watchExecutionDuration));
        assertThat(watchRecord.execution().executionDurationMs(), equalTo(watchExecutionDuration));
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        watchRecord.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        WatchRecord parsedWatchRecord = watchRecordParser().parse(watchRecord.id().value(), 0, jsonBuilder.bytes());

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedWatchRecord.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }

    @Test
    public void testParser_WithSealedWatchRecord_WithScriptSearchCondition() throws Exception {
        Watch watch = WatcherTestUtils.createTestWatch("fired_test", scriptService(), watcherHttpClient(), noopEmailService(), logger);
        ScheduleTriggerEvent event = new ScheduleTriggerEvent(watch.id(), DateTime.now(UTC), DateTime.now(UTC));
        WatchExecutionContext ctx = new TriggeredExecutionContext( watch, new DateTime(), event, timeValueSeconds(5));
        WatchRecord watchRecord = new WatchRecord(ctx.id(), watch, event);
        ctx.onActionResult(new ActionWrapper.Result("_email", new Action.Result.Failure(EmailAction.TYPE, "failed to send because blah")));
        HttpRequest request = HttpRequest.builder("localhost", 8000)
                .path("/watchfoo")
                .body("{'awesome' : 'us'}")
                .build();
        ctx.onActionResult(new ActionWrapper.Result("_webhook", new WebhookAction.Result.Success(request, new HttpResponse(300))));
        SimpleInput.Result inputResult = new SimpleInput.Result(new Payload.Simple());
        Condition.Result conditionResult = AlwaysCondition.Result.INSTANCE;
        ctx.onInputResult(inputResult);
        ctx.onConditionResult(conditionResult);
        long watchExecutionDuration = randomIntBetween(30, 100000);
        watchRecord.seal(new WatchExecutionResult(ctx, watchExecutionDuration));
        assertThat(watchRecord.execution().executionDurationMs(), equalTo(watchExecutionDuration));
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        watchRecord.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        WatchRecord parsedWatchRecord = watchRecordParser().parse(watchRecord.id().value(), 0, jsonBuilder.bytes());

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedWatchRecord.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }


}
