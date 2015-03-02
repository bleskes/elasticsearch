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

package org.elasticsearch.alerts.support;

import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.alerts.Payload;
import org.elasticsearch.common.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class Variables {

    public static final String CTX = "ctx";
    public static final String ALERT_NAME = "alert_name";
    public static final String FIRE_TIME = "fire_time";
    public static final String SCHEDULED_FIRE_TIME = "scheduled_fire_time";
    public static final String PAYLOAD = "payload";

    public static Map<String, Object> createCtxModel(ExecutionContext ctx, Payload payload) {
        return createCtxModel(ctx.alert().name(), ctx.fireTime(), ctx.scheduledTime(), payload);
    }

    public static Map<String, Object> createCtxModel(String alertName, DateTime fireTime, DateTime scheduledTime, Payload payload) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ALERT_NAME, alertName);
        vars.put(FIRE_TIME, fireTime);
        vars.put(SCHEDULED_FIRE_TIME, scheduledTime);
        vars.put(PAYLOAD, payload.data());
        Map<String, Object> model = new HashMap<>();
        model.put(CTX, vars);
        return model;
    }

}
