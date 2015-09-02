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

package org.elasticsearch.watcher.transport.action.get;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.watcher.support.xcontent.XContentSource;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
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
import static org.hamcrest.Matchers.nullValue;

/**
 *
 */
public class GetWatchTests extends AbstractWatcherIntegrationTestCase {

    @Test
    public void testGet() throws Exception {
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
        assertThat(getResponse.getStatus().version(), is(putResponse.getVersion()));
        Map<String, Object> source = getResponse.getSource().getAsMap();
        assertThat(source, notNullValue());
        assertThat(source, hasKey("trigger"));
        assertThat(source, hasKey("input"));
        assertThat(source, hasKey("condition"));
        assertThat(source, hasKey("actions"));
        assertThat(source, not(hasKey("status")));
    }

    @Test(expected = ActionRequestValidationException.class)
    public void testGet_InvalidWatchId() throws Exception {
        watcherClient().prepareGetWatch("id with whitespaces").get();
    }

    @Test
    public void testGet_NotFound() throws Exception {
        GetWatchResponse getResponse = watcherClient().getWatch(new GetWatchRequest("_name")).get();
        assertThat(getResponse, notNullValue());
        assertThat(getResponse.getId(), is("_name"));
        assertThat(getResponse.isFound(), is(false));
        assertThat(getResponse.getStatus(), nullValue());
        assertThat(getResponse.getSource(), nullValue());
        XContentSource source = getResponse.getSource();
        assertThat(source, nullValue());
    }

}
