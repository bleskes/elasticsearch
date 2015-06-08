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

import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.watch.Payload;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class Variables {

    public static final String CTX = "ctx";
    public static final String ID = "id";
    public static final String WATCH_ID = "watch_id";
    public static final String EXECUTION_TIME = "execution_time";
    public static final String TRIGGER = "trigger";
    public static final String PAYLOAD = "payload";
    public static final String METADATA = "metadata";

    public static Map<String, Object> createCtxModel(WatchExecutionContext ctx, Payload payload) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ID, ctx.id().value());
        vars.put(WATCH_ID, ctx.watch().id());
        vars.put(EXECUTION_TIME, ctx.executionTime());
        vars.put(TRIGGER, ctx.triggerEvent().data());
        if (payload != null) {
            vars.put(PAYLOAD, payload.data());
        }
        vars.put(METADATA, ctx.watch().metadata());
        Map<String, Object> model = new HashMap<>();
        model.put(CTX, vars);
        return model;
    }


}
