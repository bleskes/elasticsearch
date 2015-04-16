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

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class VariablesTests extends ElasticsearchTestCase {

    @Test
    public void testCreateCtxModel() throws Exception {
        DateTime scheduledTime = DateTime.now(UTC);
        DateTime triggeredTime = scheduledTime.plusMillis(50);
        DateTime executionTime = triggeredTime.plusMillis(50);
        Payload payload = new Payload.Simple(ImmutableMap.<String, Object>builder().put("payload_key", "payload_value").build());
        Map<String, Object> metatdata = ImmutableMap.<String, Object>builder().put("metadata_key", "metadata_value").build();
        TriggerEvent event = new ScheduleTriggerEvent("_watch_id", triggeredTime, scheduledTime);
        WatchExecutionContext wec = WatcherTestUtils.mockExecutionContextBuilder("_watch_id")
                .executionTime(executionTime)
                .triggerEvent(event)
                .payload(payload)
                .metadata(metatdata)
                .buildMock();

        Map<String, Object> model = Variables.createCtxModel(wec, payload);
        assertThat(model, notNullValue());
        assertThat(model, hasKey(Variables.CTX));
        assertThat(model.get(Variables.CTX), instanceOf(Map.class));
        assertThat(model.size(), is(1));

        Map<String, Object> ctx = (Map<String, Object>) model.get(Variables.CTX);
        assertThat(ctx, hasEntry(Variables.WATCH_ID, (Object) "_watch_id"));
        assertThat(ctx, hasEntry(Variables.EXECUTION_TIME, (Object) executionTime));
        assertThat(ctx, hasEntry(Variables.TRIGGER, (Object) event.data()));
        assertThat(ctx, hasEntry(Variables.PAYLOAD, (Object) payload.data()));
        assertThat(ctx, hasEntry(Variables.METADATA, (Object) metatdata));
        assertThat(ctx.size(), is(5));
    }
}
