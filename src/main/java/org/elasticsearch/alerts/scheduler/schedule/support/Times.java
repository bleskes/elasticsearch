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

package org.elasticsearch.alerts.scheduler.schedule.support;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;

/**
 *
 */
public interface Times extends ToXContent {

    public static final ParseField MONTH_FIELD = new ParseField("in", "month");
    public static final ParseField DAY_FIELD = new ParseField("on", "day");
    public static final ParseField TIME_FIELD = new ParseField("at", "time");
    public static final ParseField HOUR_FIELD = new ParseField("hour");
    public static final ParseField MINUTE_FIELD = new ParseField("minute");

}
