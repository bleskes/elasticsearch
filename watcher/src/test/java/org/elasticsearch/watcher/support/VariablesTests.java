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

package org.elasticsearch.watcher.support;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.Test;

import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;
import static org.elasticsearch.watcher.test.WatcherTestUtils.assertValue;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class VariablesTests extends ESTestCase {

    @Test
    public void testCreateCtxModel() throws Exception {
        DateTime scheduledTime = DateTime.now(UTC);
        DateTime triggeredTime = scheduledTime.plusMillis(50);
        DateTime executionTime = triggeredTime.plusMillis(50);
        Payload payload = new Payload.Simple(ImmutableMap.<String, Object>builder().put("payload_key", "payload_value").build());
        Map<String, Object> metatdata = ImmutableMap.<String, Object>builder().put("metadata_key", "metadata_value").build();
        TriggerEvent event = new ScheduleTriggerEvent("_watch_id", triggeredTime, scheduledTime);
        Wid wid = new Wid("_watch_id", 0, executionTime);
        WatchExecutionContext ctx = WatcherTestUtils.mockExecutionContextBuilder("_watch_id")
                .wid(wid)
                .executionTime(executionTime)
                .triggerEvent(event)
                .payload(payload)
                .metadata(metatdata)
                .buildMock();

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        assertThat(model, notNullValue());
        assertThat(model.size(), is(1));
        assertValue(model, "ctx", instanceOf(Map.class));
        assertValue(model, "ctx.id", is(wid.value()));
        assertValue(model, "ctx.execution_time", is(executionTime));
        assertValue(model, "ctx.trigger", is(event.data()));
        assertValue(model, "ctx.payload", is(payload.data()));
        assertValue(model, "ctx.metadata", is(metatdata));
    }
}
