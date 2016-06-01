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

package org.elasticsearch.xpack.watcher.test;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.trigger.TriggerEvent;
import org.elasticsearch.xpack.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class WatchExecutionContextMockBuilder {

    private final WatchExecutionContext ctx;
    private final Watch watch;

    public WatchExecutionContextMockBuilder(String watchId) {
        ctx = mock(WatchExecutionContext.class);
        watch = mock(Watch.class);
        when(watch.id()).thenReturn(watchId);
        when(ctx.watch()).thenReturn(watch);
        payload(Collections.<String, Object>emptyMap());
        metadata(Collections.<String, Object>emptyMap());
        time(watchId, DateTime.now(UTC));
    }

    public WatchExecutionContextMockBuilder wid(Wid wid) {
        when(ctx.id()).thenReturn(wid);
        return this;
    }

    public WatchExecutionContextMockBuilder payload(String key, Object value) {
        return payload(new Payload.Simple(MapBuilder.<String, Object>newMapBuilder().put(key, value).map()));
    }

    public WatchExecutionContextMockBuilder payload(Map<String, Object> payload) {
        return payload(new Payload.Simple(payload));
    }

    public WatchExecutionContextMockBuilder payload(Payload payload) {
        when(ctx.payload()).thenReturn(payload);
        return this;
    }

    public WatchExecutionContextMockBuilder time(String watchId, DateTime time) {
        return executionTime(time).triggerEvent(new ScheduleTriggerEvent(watchId, time, time));
    }

    public WatchExecutionContextMockBuilder executionTime(DateTime time) {
        when(ctx.executionTime()).thenReturn(time);
        return this;
    }

    public WatchExecutionContextMockBuilder triggerEvent(TriggerEvent event) {
        when(ctx.triggerEvent()).thenReturn(event);
        return this;
    }

    public WatchExecutionContextMockBuilder metadata(Map<String, Object> metadata) {
        when(watch.metadata()).thenReturn(metadata);
        return this;
    }

    public WatchExecutionContextMockBuilder metadata(String key, String value) {
        return metadata(MapBuilder.<String, Object>newMapBuilder().put(key, value).map());
    }

    public WatchExecutionContext buildMock() {
        return ctx;
    }
}
