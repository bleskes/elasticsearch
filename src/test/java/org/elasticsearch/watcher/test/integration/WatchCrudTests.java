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

package org.elasticsearch.watcher.test.integration;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.support.xcontent.XContentSource;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.watcher.transport.actions.get.GetWatchRequest;
import org.elasticsearch.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class WatchCrudTests extends AbstractWatcherIntegrationTests {

    @Test @Repeat(iterations = 10)
    public void testPut() throws Exception {
        ensureWatcherStarted();

        WatchSourceBuilder source = watchBuilder()
                .trigger(schedule(interval("5m")));

        if (randomBoolean()) {
            source.input(simpleInput());
        }
        if (randomBoolean()) {
            source.condition(alwaysCondition());
        }
        if (randomBoolean()) {
            source.addAction("_action1", loggingAction("{{ctx.watch_id}}"));
        }

        PutWatchResponse response = watcherClient().preparePutWatch("_name").setSource(source).get();

        assertThat(response, notNullValue());
        assertThat(response.isCreated(), is(true));
        assertThat(response.getVersion(), is(1L));
    }

    @Test(expected = WatchSourceBuilder.BuilderException.class)
    public void testPut_NoTrigger() throws Exception {
        ensureWatcherStarted();
        watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .input(simpleInput())
                .condition(alwaysCondition())
                .addAction("_action1", loggingAction("{{ctx.watch_id}}")))
                .get();
    }

    @Test(expected = ActionRequestValidationException.class)
    public void testPut_InvalidWatchId() throws Exception {
        ensureWatcherStarted();
        watcherClient().preparePutWatch("id with whitespaces").setSource(watchBuilder()
                .trigger(schedule(interval("5m"))))
                .get();
    }

    @Test(expected = WatcherException.class)
    public void testPut_InvalidActionId() throws Exception {
        ensureWatcherStarted();
        watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .trigger(schedule(interval("5m")))
                .addAction("id with whitespaces", loggingAction("{{ctx.watch_id}}")))
                .get();
    }

    @Test
    public void testGet() throws Exception {
        ensureWatcherStarted();
        PutWatchResponse putResponse = watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .trigger(schedule(interval("5m")))
                .input(simpleInput())
                .condition(alwaysCondition())
                .addAction("_action1", loggingAction("{{ctx.watch_id}}")))
                .get();

        assertThat(putResponse, notNullValue());
        assertThat(putResponse.isCreated(), is(true));

        GetWatchResponse getResponse = watcherClient().getWatch(new GetWatchRequest("_name")).get();
        assertThat(getResponse, notNullValue());
        assertThat(getResponse.isFound(), is(true));
        assertThat(getResponse.getId(), is("_name"));
        assertThat(getResponse.getVersion(), is(putResponse.getVersion()));
        Map<String, Object> source = getResponse.getSource().getAsMap();
        assertThat(source, notNullValue());
        assertThat(source, hasKey("trigger"));
        assertThat(source, hasKey("input"));
        assertThat(source, hasKey("condition"));
        assertThat(source, hasKey("actions"));
        assertThat(source, hasKey("status"));
    }

    @Test(expected = ActionRequestValidationException.class)
    public void testGet_InvalidWatchId() throws Exception {
        watcherClient().prepareGetWatch("id with whitespaces").get();
    }

    @Test
    public void testGet_NotFound() throws Exception {
        ensureWatcherStarted();

        GetWatchResponse getResponse = watcherClient().getWatch(new GetWatchRequest("_name")).get();
        assertThat(getResponse, notNullValue());
        assertThat(getResponse.getId(), is("_name"));
        assertThat(getResponse.getVersion(), is(-1L));
        assertThat(getResponse.isFound(), is(false));
        assertThat(getResponse.getSource(), nullValue());
        XContentSource source = getResponse.getSource();
        assertThat(source, nullValue());
    }

    @Test
    public void testDelete() throws Exception {
        ensureWatcherStarted();
        PutWatchResponse putResponse = watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .trigger(schedule(interval("5m")))
                .input(simpleInput())
                .condition(alwaysCondition())
                .addAction("_action1", loggingAction("{{ctx.watch_id}}")))
                .get();

        assertThat(putResponse, notNullValue());
        assertThat(putResponse.isCreated(), is(true));

        DeleteWatchResponse deleteResponse = watcherClient().deleteWatch(new DeleteWatchRequest("_name")).get();
        assertThat(deleteResponse, notNullValue());
        assertThat(deleteResponse.getId(), is("_name"));
        assertThat(deleteResponse.getVersion(), is(putResponse.getVersion() + 1));
        assertThat(deleteResponse.isFound(), is(true));
    }

    @Test
    public void testDelete_NotFound() throws Exception {
        DeleteWatchResponse response = watcherClient().deleteWatch(new DeleteWatchRequest("_name")).get();
        assertThat(response, notNullValue());
        assertThat(response.getId(), is("_name"));
        assertThat(response.getVersion(), is(1L));
        assertThat(response.isFound(), is(false));
    }

    @Test(expected = ActionRequestValidationException.class)
    public void testDelete_InvalidWatchId() throws Exception {
        watcherClient().deleteWatch(new DeleteWatchRequest("id with whitespaces")).actionGet();
    }
}
