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

package org.elasticsearch.alerts;

import org.elasticsearch.alerts.actions.Action;
import org.elasticsearch.alerts.throttle.Throttler;
import org.elasticsearch.alerts.transform.Transform;
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.common.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class AlertContext {

    private final Alert alert;
    private final DateTime fireTime;
    private final DateTime scheduledTime;

    private Payload payload;
    private Trigger.Result triggerResult;
    private Throttler.Result throttleResult;
    private Transform.Result transformResult;
    private Map<String, Action.Result> actionsResults = new HashMap<>();

    public AlertContext(Alert alert, DateTime fireTime, DateTime scheduledTime) {
        this.alert = alert;
        this.fireTime = fireTime;
        this.scheduledTime = scheduledTime;
    }

    public Alert alert() {
        return alert;
    }

    public DateTime fireTime() {
        return fireTime;
    }

    public DateTime scheduledTime() {
        return scheduledTime;
    }

    public Payload payload() {
        return payload;
    }

    public void triggerResult(Trigger.Result triggerResult) {
        this.triggerResult = triggerResult;
        this.payload = triggerResult.payload();
    }

    public Trigger.Result triggerResult() {
        return triggerResult;
    }

    public void throttleResult(Throttler.Result throttleResult) {
        this.throttleResult = throttleResult;
    }

    public Throttler.Result throttleResult() {
        return throttleResult;
    }

    public void transformResult(Transform.Result transformResult) {
        this.transformResult = transformResult;
        this.payload = transformResult.payload();
    }

    public Transform.Result transformResult() {
        return transformResult;
    }

    public void addActionResult(Action.Result result) {
        actionsResults.put(result.type(), result);
    }

    public Map<String, Action.Result> actionsResults() {
        return actionsResults;
    }

}
