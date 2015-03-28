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

import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.WatchExecutionContext;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.common.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class Variables {

    public static final String CTX = "ctx";
    public static final String WATCH_NAME = "watch_name";
    public static final String EXECUTION_TIME = "execution_time";
    public static final String TRIGGER = "trigger";
    public static final String PAYLOAD = "payload";

    public static Map<String, Object> createCtxModel(WatchExecutionContext ctx, Payload payload) {
        return createCtxModel(ctx.watch().name(), ctx.executionTime(), ctx.triggerEvent(), payload);
    }

    public static Map<String, Object> createCtxModel(String watchName, DateTime executionTime, TriggerEvent triggerEvent, Payload payload) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(WATCH_NAME, watchName);
        vars.put(EXECUTION_TIME, executionTime);
        vars.put(TRIGGER, triggerEvent.data());
        if (payload != null) {
            vars.put(PAYLOAD, payload.data());
        }
        Map<String, Object> model = new HashMap<>();
        model.put(CTX, vars);
        return model;
    }

}
