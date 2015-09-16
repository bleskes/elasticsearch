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

package org.elasticsearch.watcher.transport.action.put;


import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.junit.Test;

import static org.elasticsearch.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class PutWatchTests extends AbstractWatcherIntegrationTestCase {

    @Test

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

    @Test(expected = IllegalStateException.class)
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

    @Test(expected = ElasticsearchParseException.class)
    public void testPut_InvalidActionId() throws Exception {
        ensureWatcherStarted();
        watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .trigger(schedule(interval("5m")))
                .addAction("id with whitespaces", loggingAction("{{ctx.watch_id}}")))
                .get();
    }


}
