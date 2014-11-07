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
import org.elasticsearch.alerts.actions.AlertActionRegistry;
import org.elasticsearch.alerts.triggers.AlertTrigger;
import org.elasticsearch.common.io.stream.DataOutputStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Alert implements ToXContent {

    private String alertName;
    private SearchRequest searchRequest;
    private AlertTrigger trigger;
    private List<AlertAction> actions;
    private String schedule;
    private DateTime lastActionFire;
    private long version;
    private boolean enabled;

    public Alert() {
    }

    public Alert(String alertName, SearchRequest searchRequest, AlertTrigger trigger, List<AlertAction> actions, String schedule, DateTime lastActionFire, long version, boolean enabled) {
        this.alertName = alertName;
        this.searchRequest = searchRequest;
        this.trigger = trigger;
        this.actions = actions;
        this.schedule = schedule;
        this.lastActionFire = lastActionFire;
        this.version = version;
        this.enabled = enabled;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        searchRequest.writeTo(new DataOutputStreamOutput(new DataOutputStream(out)));
        builder.field(AlertsStore.REQUEST_BINARY_FIELD.getPreferredName(), out.toByteArray());
        builder.field(AlertsStore.SCHEDULE_FIELD.getPreferredName(), schedule);
        builder.field(AlertsStore.ENABLE.getPreferredName(), enabled);
        if (lastActionFire != null) {
            builder.field(AlertsStore.LAST_ACTION_FIRE.getPreferredName(), lastActionFire);
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
            trigger.toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }

    public void readFrom(StreamInput in) throws IOException {
        alertName = in.readString();
        searchRequest = new SearchRequest();
        searchRequest.readFrom(in);
        trigger = AlertTrigger.readFrom(in);
        int numActions = in.readInt();
        actions = new ArrayList<>(numActions);
        for (int i=0; i<numActions; ++i) {
            actions.add(AlertActionRegistry.readFrom(in));
        }
        schedule = in.readOptionalString();
        if (in.readBoolean()) {
            lastActionFire = new DateTime(in.readLong(), DateTimeZone.UTC);
        }
        version = in.readLong();
        enabled = in.readBoolean();
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(alertName);
        searchRequest.writeTo(out);
        AlertTrigger.writeTo(trigger, out);
        if (actions == null) {
            out.writeInt(0);
        } else {
            out.writeInt(actions.size());
            for (AlertAction action : actions) {
                action.writeTo(out);
            }
        }
        out.writeOptionalString(schedule);
        if (lastActionFire == null) {
            out.writeBoolean(false);
        } else {
            out.writeLong(lastActionFire.toDateTime(DateTimeZone.UTC).getMillis());
        }
        out.writeLong(version);
        out.writeBoolean(enabled);
    }


    /**
     * @return The last time this alert ran.
     */
    public DateTime lastActionFire() {
        return lastActionFire;
    }

    public void lastActionFire(DateTime lastActionFire) {
        this.lastActionFire = lastActionFire;
    }

    /**
     * @return Whether this alert has been enabled.
     */
    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
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
}
