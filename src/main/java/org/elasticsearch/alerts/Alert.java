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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.actions.AlertAction;
import org.elasticsearch.alerts.triggers.AlertTrigger;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Alert implements ToXContent {

    private String alertName;
    private SearchRequest searchRequest;
    private AlertTrigger trigger;
    private List<AlertAction> actions;
    private String schedule;
    private DateTime lastExecuteTime;
    private TimeValue throttlePeriod = new TimeValue(0);
    private DateTime timeLastActionExecuted = null;
    private AlertAckState ackState = AlertAckState.NOT_ACKABLE;
    private Map<String,Object> metadata = null;

    private transient long version;
    private transient XContentType contentType;

    public Alert() {
        actions = new ArrayList<>();
    }


    public Alert(String alertName, SearchRequest searchRequest, AlertTrigger trigger, List<AlertAction> actions, String schedule, DateTime lastExecuteTime, long version, TimeValue throttlePeriod, AlertAckState ackState) {
        this.alertName = alertName;
        this.searchRequest = searchRequest;
        this.trigger = trigger;
        this.actions = actions;
        this.schedule = schedule;
        this.lastExecuteTime = lastExecuteTime;
        this.version = version;
        this.throttlePeriod = throttlePeriod;
        this.ackState = ackState;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(AlertsStore.SCHEDULE_FIELD.getPreferredName(), schedule);
        builder.field(AlertsStore.REQUEST_FIELD.getPreferredName());
        AlertUtils.writeSearchRequest(searchRequest, builder, params);
        builder.field(AlertsStore.THROTTLE_PERIOD_FIELD.getPreferredName(), throttlePeriod.millis());
        builder.field(AlertsStore.ACK_STATE_FIELD.getPreferredName(), ackState.toString());

        if (timeLastActionExecuted != null) {
            builder.field(AlertsStore.LAST_ACTION_EXECUTED_FIELD.getPreferredName(), timeLastActionExecuted);
        }

        if (lastExecuteTime != null) {
            builder.field(AlertsStore.LAST_ACTION_FIRE.getPreferredName(), lastExecuteTime);
        }

        if (actions != null && !actions.isEmpty()) {
            builder.startObject(AlertsStore.ACTION_FIELD.getPreferredName());
            for (AlertAction action : actions){
                builder.field(action.getActionName());
                action.toXContent(builder, params);
            }
            builder.endObject();
        }
        if (trigger != null) {
            builder.field(AlertsStore.TRIGGER_FIELD.getPreferredName());
            builder.startObject();
            builder.field(trigger.getTriggerName());
            trigger.toXContent(builder, params);
            builder.endObject();
        }
        if (metadata != null) {
            builder.field(AlertsStore.META_FIELD.getPreferredName(), metadata);
        }
        builder.endObject();
        return builder;
    }

    /**
     * @return The last time this alert ran.
     */
    public DateTime lastExecuteTime() {
        return lastExecuteTime;
    }

    public void lastExecuteTime(DateTime lastActionFire) {
        this.lastExecuteTime = lastActionFire;
    }

    /**
     * @return The current version of the alert. (es document version)
     */
    public long version() {
        return version;
    }

    public void version(long version) {
        this.version = version;
    }

    void setContentType(XContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * @return xcontext type of the _source of this action entry.
     */
    public XContentType getContentType() {
        return contentType;
    }

    /**
     * @return The unique name of this alert.
     */
    public String alertName() {
        return alertName;
    }

    public void alertName(String alertName) {
        this.alertName = alertName;
    }

    /**
     * @return The search request that runs when the alert runs by the sc
     */
    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    /**
     * @return The trigger that is going to evaluate if the alert is going to execute the alert actions.
     */
    public AlertTrigger trigger() {
        return trigger;
    }

    public void trigger(AlertTrigger trigger) {
        this.trigger = trigger;
    }

    /**
     * @return the actions to be executed if the alert matches the trigger
     */
    public List<AlertAction> actions() {
        return actions;
    }

    public void actions(List<AlertAction> action) {
        this.actions = action;
    }

    /**
     * @return The cron schedule expression that expresses when to run the alert.
     */
    public String schedule() {
        return schedule;
    }

    public void schedule(String schedule) {
        this.schedule = schedule;
    }

    /**
     * @return The time the last action was executed
     */
    public DateTime getTimeLastActionExecuted() {
        return timeLastActionExecuted;
    }

    public void setTimeLastActionExecuted(DateTime timeLastActionExecuted) {
        this.timeLastActionExecuted = timeLastActionExecuted;
    }

    /**
     * @return the minimum time between action executions
     */
    public TimeValue getThrottlePeriod() {
        return throttlePeriod;
    }

    public void setThrottlePeriod(TimeValue throttlePeriod) {
        this.throttlePeriod = throttlePeriod;
    }

    /**
     * @return the ack state of this alert
     */
    public AlertAckState getAckState() {
        return ackState;
    }

    public void setAckState(AlertAckState ackState) {
        this.ackState = ackState;
    }

    /**
     * @return The metadata that was associated with the alert
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alert alert = (Alert) o;
        return alert.alertName().equals(alertName);
    }

    @Override
    public int hashCode() {
        return alertName.hashCode();
    }

}
