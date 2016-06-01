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

package org.elasticsearch.xpack.trigger;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.support.DateTimeUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class TriggerEvent implements ToXContent {

    private final String jobName;
    protected final DateTime triggeredTime;
    protected final Map<String, Object> data;

    public TriggerEvent(String jobName, DateTime triggeredTime) {
        this.jobName = jobName;
        this.triggeredTime = triggeredTime;
        this.data = new HashMap<>();
        data.put(Field.TRIGGERED_TIME.getPreferredName(), triggeredTime);
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

    @Override
    public String toString() {
        return new StringBuilder("[")
                .append("name=[").append(jobName).append("],")
                .append("triggered_time=[").append(triggeredTime).append("],")
                .append("data=[").append(data).append("]")
                .append("]")
                .toString();
    }

    public void recordXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Field.TYPE.getPreferredName(), type());
        DateTimeUtils.writeDate(Field.TRIGGERED_TIME.getPreferredName(), builder, triggeredTime);
        recordDataXContent(builder, params);
        builder.endObject();
    }

    public abstract void recordDataXContent(XContentBuilder builder, Params params) throws IOException;

    protected interface Field {
        ParseField TYPE = new ParseField("type");
        ParseField TRIGGERED_TIME = new ParseField("triggered_time");
    }

}
