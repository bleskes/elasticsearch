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

package org.elasticsearch.watcher.trigger;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class TriggerEvent implements ToXContent {

    public static final ParseField TRIGGERED_TIME_FIELD = new ParseField("triggered_time");

    private final String jobName;
    protected final DateTime triggeredTime;
    protected final Map<String, Object> data;

    public TriggerEvent(String jobName, DateTime triggeredTime) {
        this.jobName = jobName;
        this.triggeredTime = triggeredTime;
        this.data = new HashMap<>();
        data.put(TRIGGERED_TIME_FIELD.getPreferredName(), triggeredTime);
    }

    public String jobName() {
        return jobName;
    }

    public abstract String type();

    public DateTime triggeredTime() {
        return triggeredTime;
    }

    public final Map<String, Object> data() {
        return data;
    }

}
