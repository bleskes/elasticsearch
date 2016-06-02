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

package org.elasticsearch.xpack.watcher.transport.action.execute;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.execution.ActionExecutionMode;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.ack.AckWatchRequestBuilder;
import org.elasticsearch.xpack.watcher.transport.actions.ack.AckWatchResponse;
import org.elasticsearch.xpack.watcher.transport.actions.execute.ExecuteWatchRequestBuilder;
import org.elasticsearch.xpack.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.xpack.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.xpack.watcher.condition.ConditionBuilders.neverCondition;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.cron;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class ExecuteWatchTests extends AbstractWatcherIntegrationTestCase {
    public void testExecuteInvalidWatchId() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        try {
            watcherClient().prepareExecuteWatch("id with whitespaces")
                    .setTriggerEvent(new ScheduleTriggerEvent(now, now))
                    .get();
            fail("Expected ActionRequestValidationException");
        } catch (ActionRequestValidationException e) {
            assertThat(e.getMessage(), containsString("Watch id cannot have white spaces"));
        }
    }

    public void testExecuteInvalidActionId() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        try {
            watcherClient().prepareExecuteWatch("_id")
                    .setTriggerEvent(new ScheduleTriggerEvent(now, now))
                    .setActionMode("id with whitespaces", randomFrom(ActionExecutionMode.values()))
                    .get();
            fail("Expected ActionRequestValidationException");
        } catch (ActionRequestValidationException e) {
            assertThat(e.getMessage(), containsString("Action id cannot have white spaces"));
        }
    }

    public void testExecuteAllDefaults() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? 2099")))
                        .input(simpleInput("foo", "bar"))
                        .condition(alwaysCondition())
                        .addAction("log", loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        ExecuteWatchResponse response = watcherClient.prepareExecuteWatch("_id").get();
        assertThat(response, notNullValue());
        assertThat(response.getRecordId(), notNullValue());
        Wid wid = new Wid(response.getRecordId());
        assertThat(wid.watchId(), is("_id"));

        XContentSource record = response.getRecordSource();
        assertValue(record, "watch_id", is("_id"));
        assertValue(record, "trigger_event.type", is("manual"));
        assertValue(record, "trigger_event.triggered_time", notNullValue());
        String triggeredTime = record.getValue("trigger_event.triggered_time");
        assertValue(record, "trigger_event.manual.schedule.scheduled_time", is(triggeredTime));
        assertValue(record, "state", is("executed"));
        assertValue(record, "input.simple.foo", is("bar"));
        assertValue(record, "condition.always", notNullValue());
        assertValue(record, "result.execution_time", notNullValue());
        assertValue(record, "result.execution_duration", notNullValue());
        assertValue(record, "result.input.type", is("simple"));
        assertValue(record, "result.input.payload.foo", is("bar"));
        assertValue(record, "result.condition.type", is("always"));
        assertValue(record, "result.condition.met", is(true));
        assertValue(record, "result.actions.0.id", is("log"));
        assertValue(record, "result.actions.0.type", is("logging"));
        assertValue(record, "result.actions.0.status", is("success"));
        assertValue(record, "result.actions.0.logging.logged_text", is("_text"));
    }

    public void testExecuteCustomTriggerData() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? 2099")))
                        .input(simpleInput("foo", "bar"))
                        .condition(alwaysCondition())
                        .addAction("log", loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        DateTime triggeredTime = DateTime.now(DateTimeZone.UTC);
        DateTime scheduledTime = randomBoolean() ? triggeredTime.minusDays(1) : triggeredTime;

        ExecuteWatchRequestBuilder requestBuilder = watcherClient.prepareExecuteWatch("_id");
        if (randomBoolean()) {
            Map<String, Object> data = new HashMap<>();
            data.put("triggered_time", WatcherDateTimeUtils.formatDate(triggeredTime));
            if (scheduledTime != triggeredTime) {
                data.put("scheduled_time", WatcherDateTimeUtils.formatDate(scheduledTime));
            }
            requestBuilder.setTriggerData(data);
        } else {
            ScheduleTriggerEvent event = new ScheduleTriggerEvent(triggeredTime, scheduledTime);
            requestBuilder.setTriggerEvent(event);
        }
        ExecuteWatchResponse response = requestBuilder.get();

        assertThat(response, notNullValue());
        assertThat(response.getRecordId(), notNullValue());
        Wid wid = new Wid(response.getRecordId());
        assertThat(wid.watchId(), is("_id"));

        XContentSource record = response.getRecordSource();
        assertValue(record, "watch_id", is("_id"));
        assertValue(record, "trigger_event.type", is("manual"));
        assertValue(record, "trigger_event.triggered_time", is(WatcherDateTimeUtils.formatDate(triggeredTime)));
        assertValue(record, "trigger_event.manual.schedule.scheduled_time", is(WatcherDateTimeUtils.formatDate(scheduledTime)));
        assertValue(record, "state", is("executed"));
        assertValue(record, "input.simple.foo", is("bar"));
        assertValue(record, "condition.always", notNullValue());
        assertValue(record, "result.execution_time", notNullValue());
        assertValue(record, "result.execution_duration", notNullValue());
        assertValue(record, "result.input.type", is("simple"));
        assertValue(record, "result.input.payload.foo", is("bar"));
        assertValue(record, "result.condition.type", is("always"));
        assertValue(record, "result.condition.met", is(true));
        assertValue(record, "result.actions.0.id", is("log"));
        assertValue(record, "result.actions.0.type", is("logging"));
        assertValue(record, "result.actions.0.status", is("success"));
        assertValue(record, "result.actions.0.logging.logged_text", is("_text"));
    }

    public void testExecuteAlternativeInput() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? 2099")))
                        .input(simpleInput("foo", "bar"))
                        .condition(alwaysCondition())
                        .addAction("log", loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        ExecuteWatchResponse response = watcherClient.prepareExecuteWatch("_id")
                .setAlternativeInput(singletonMap("foo1", "bar1"))
                .get();
        assertThat(response, notNullValue());
        assertThat(response.getRecordId(), notNullValue());
        Wid wid = new Wid(response.getRecordId());
        assertThat(wid.watchId(), is("_id"));

        XContentSource record = response.getRecordSource();
        assertValue(record, "watch_id", is("_id"));
        assertValue(record, "trigger_event.type", is("manual"));
        assertValue(record, "trigger_event.triggered_time", notNullValue());
        String triggeredTime = record.getValue("trigger_event.triggered_time");
        assertValue(record, "trigger_event.manual.schedule.scheduled_time", is(triggeredTime));
        assertValue(record, "state", is("executed"));
        assertValue(record, "input.simple.foo", is("bar")); // this is the original input
        assertValue(record, "condition.always", notNullValue());
        assertValue(record, "result.execution_time", notNullValue());
        assertValue(record, "result.execution_duration", notNullValue());
        assertValue(record, "result.input.type", is("simple"));
        assertValue(record, "result.input.payload.foo1", is("bar1")); // this is the alternative one
        assertValue(record, "result.condition.type", is("always"));
        assertValue(record, "result.condition.met", is(true));
        assertValue(record, "result.actions.0.id", is("log"));
        assertValue(record, "result.actions.0.type", is("logging"));
        assertValue(record, "result.actions.0.status", is("success"));
        assertValue(record, "result.actions.0.logging.logged_text", is("_text"));
    }

    public void testExecuteIgnoreCondition() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? 2099")))
                        .input(simpleInput("foo", "bar"))
                        .condition(neverCondition())
                        .addAction("log", loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        ExecuteWatchResponse response = watcherClient.prepareExecuteWatch("_id")
                .setIgnoreCondition(true)
                .get();

        assertThat(response, notNullValue());
        assertThat(response.getRecordId(), notNullValue());
        Wid wid = new Wid(response.getRecordId());
        assertThat(wid.watchId(), is("_id"));

        XContentSource record = response.getRecordSource();
        assertValue(record, "watch_id", is("_id"));
        assertValue(record, "trigger_event.type", is("manual"));
        assertValue(record, "trigger_event.triggered_time", notNullValue());
        String triggeredTime = record.getValue("trigger_event.triggered_time");
        assertValue(record, "trigger_event.manual.schedule.scheduled_time", is(triggeredTime));
        assertValue(record, "state", is("executed"));
        assertValue(record, "input.simple.foo", is("bar"));
        assertValue(record, "condition.never", notNullValue()); // the original condition
        assertValue(record, "result.execution_time", notNullValue());
        assertValue(record, "result.execution_duration", notNullValue());
        assertValue(record, "result.input.type", is("simple"));
        assertValue(record, "result.input.payload.foo", is("bar"));
        assertValue(record, "result.condition.type", is("always")); // when ignored, the condition is replaced with "always"
        assertValue(record, "result.condition.met", is(true));
        assertValue(record, "result.actions.0.id", is("log"));
        assertValue(record, "result.actions.0.type", is("logging"));
        assertValue(record, "result.actions.0.status", is("success"));
        assertValue(record, "result.actions.0.logging.logged_text", is("_text"));
    }

    public void testExecuteActionMode() throws Exception {
        final WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(interval("1s"))) // run every second so we can ack it
                        .input(simpleInput("foo", "bar"))
                        .defaultThrottlePeriod(TimeValue.timeValueMillis(0))
                        .condition(alwaysCondition())
                        .addAction("log", loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        boolean execute = randomBoolean();
        boolean force = randomBoolean();
        ActionExecutionMode mode;
        if (randomBoolean()) {
            mode = ActionExecutionMode.SKIP;
        } else {
            if (execute && force) {
                mode = ActionExecutionMode.FORCE_EXECUTE;
            } else if (execute) {
                mode = ActionExecutionMode.EXECUTE;
            } else if (force) {
                mode = ActionExecutionMode.FORCE_SIMULATE;
            } else {
                mode = ActionExecutionMode.SIMULATE;
            }
        }

        if (mode.force()) {
            // since we're forcing, lets ack the action, such that it'd suppoed to be throttled
            // but forcing will ignore the throttling

            // lets wait for the watch to be ackable
            if (timeWarped()) {
                timeWarp().scheduler().trigger("_id");
            } else {
                assertBusy(new Runnable() {
                    @Override
                    public void run() {
                        GetWatchResponse getWatchResponse = watcherClient.prepareGetWatch("_id").get();
                        assertThat(getWatchResponse.getStatus().actionStatus("log").ackStatus().state(),
                                equalTo(ActionStatus.AckStatus.State.ACKABLE));
                    }
                });
            }

            String[] actionIds = randomFrom(
                    new String[] { "_all" },
                    new String[] { "log" },
                    new String[] { "foo", "_all" },
                    null
            );
            AckWatchRequestBuilder ackWatchRequestBuilder = watcherClient.prepareAckWatch("_id");
            if (actionIds != null) {
                ackWatchRequestBuilder.setActionIds(actionIds);
            }
            AckWatchResponse ackWatchResponse = ackWatchRequestBuilder.get();
            assertThat(ackWatchResponse, notNullValue());
            WatchStatus status = ackWatchResponse.getStatus();
            assertThat(status, notNullValue());
            ActionStatus actionStatus = status.actionStatus("log");
            assertThat(actionStatus, notNullValue());
            assertThat(actionStatus.ackStatus().state(), is(ActionStatus.AckStatus.State.ACKED));
        }

        ExecuteWatchResponse response = watcherClient.prepareExecuteWatch("_id")
                .setActionMode(randomBoolean() ? "log" : "_all", mode)
                .get();
        assertThat(response, notNullValue());
        assertThat(response.getRecordId(), notNullValue());
        Wid wid = new Wid(response.getRecordId());
        assertThat(wid.watchId(), is("_id"));

        XContentSource record = response.getRecordSource();
        assertValue(record, "watch_id", is("_id"));
        assertValue(record, "trigger_event.type", is("manual"));
        assertValue(record, "trigger_event.triggered_time", notNullValue());
        String triggeredTime = record.getValue("trigger_event.triggered_time");
        assertValue(record, "trigger_event.manual.schedule.scheduled_time", is(triggeredTime));
        if (mode == ActionExecutionMode.SKIP) {
            assertValue(record, "state", is("throttled"));
        } else {
            assertValue(record, "state", is("executed"));
        }
        assertValue(record, "input.simple.foo", is("bar"));
        assertValue(record, "condition.always", notNullValue());
        assertValue(record, "result.execution_time", notNullValue());
        assertValue(record, "result.execution_duration", notNullValue());
        assertValue(record, "result.input.type", is("simple"));
        assertValue(record, "result.input.payload.foo", is("bar"));
        assertValue(record, "result.condition.type", is("always"));
        assertValue(record, "result.condition.met", is(true));
        assertValue(record, "result.actions.0.id", is("log"));
        assertValue(record, "result.actions.0.type", is("logging"));
        switch (mode) {
            case SKIP: // the action should be manually skipped/throttled
                assertValue(record, "result.actions.0.status", is("throttled"));
                assertValue(record, "result.actions.0.reason", is("manually skipped"));
                break;
            default:
                if (mode.simulate()) {
                    assertValue(record, "result.actions.0.status", is("simulated"));
                } else {
                    assertValue(record, "result.actions.0.status", is("success"));
                }
                assertValue(record, "result.actions.0.logging.logged_text", is("_text"));
        }
    }
}
