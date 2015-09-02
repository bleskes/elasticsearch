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

package org.elasticsearch.watcher.transport.action.delete;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
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
public class DeleteWatchTests extends AbstractWatcherIntegrationTestCase {

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
