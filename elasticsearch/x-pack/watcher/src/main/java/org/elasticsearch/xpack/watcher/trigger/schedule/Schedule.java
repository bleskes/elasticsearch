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

package org.elasticsearch.xpack.watcher.trigger.schedule;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public interface Schedule extends ToXContent {

    String type();

    /**
     * Returns the next scheduled time after the given time, according to this schedule. If the given schedule
     * cannot resolve the next scheduled time, then {@code -1} is returned. It really depends on the type of
     * schedule to determine when {@code -1} is returned. Some schedules (e.g. IntervalSchedule) will never return
     * {@code -1} as they can always compute the next scheduled time. {@code Cron} based schedules are good example
     * of schedules that may return {@code -1}, for example, when the schedule only points to times that are all
     * before the given time (in which case, there is no next scheduled time for the given time).
     *
     * Example:
     *
     *      cron    0 0 0 * 1 ? 2013        (only points to days in January 2013)
     *
     *      time    2015-01-01 12:00:00     (this time is in 2015)
     *
     */
    long nextScheduledTimeAfter(long startTime, long time);

    interface Parser<S extends Schedule> {

        String type();

        S parse(XContentParser parser) throws IOException;
    }
}
