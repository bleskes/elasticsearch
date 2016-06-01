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

package org.elasticsearch.xpack.watcher.support;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.watcher.test.WatcherTestUtils;
import org.elasticsearch.xpack.trigger.TriggerEvent;
import org.elasticsearch.xpack.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.assertValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTimeZone.UTC;

/**
 *
 */
public class VariablesTests extends ESTestCase {
    public void testCreateCtxModel() throws Exception {
        DateTime scheduledTime = DateTime.now(UTC);
        DateTime triggeredTime = scheduledTime.plusMillis(50);
        DateTime executionTime = triggeredTime.plusMillis(50);
        Payload payload = new Payload.Simple(singletonMap("payload_key", "payload_value"));
        Map<String, Object> metatdata = singletonMap("metadata_key", "metadata_value");
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
