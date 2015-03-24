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

package org.elasticsearch.watcher.scheduler.schedule;

/**
 * A static factory for all available schedules.
 */
public class Schedules {

    private Schedules() {
    }

    /**
     * Creates an interval schedule. The provided string can have the following format:
     * <ul>
     *     <li>34s</li> - a 34 seconds long interval
     *     <li>23m</li> - a 23 minutes long interval
     *     <li>40h</li> - a 40 hours long interval
     *     <li>63d</li> - a 63 days long interval
     *     <li>27w</li> - a 27 weeks long interval
     * </ul>
     *
     * @param interval  The fixed interval by which the schedule will trigger.
     * @return          The newly created interval schedule
     */
    public static IntervalSchedule interval(String interval) {
        return new IntervalSchedule(IntervalSchedule.Interval.parse(interval));
    }

    /**
     * Creates an interval schedule.
     *
     * @param duration  The duration of the interval
     * @param unit      The unit of the duration (seconds, minutes, hours, days or weeks)
     * @return          The newly created interval schedule.
     */
    public static IntervalSchedule interval(long duration, IntervalSchedule.Interval.Unit unit) {
        return new IntervalSchedule(new IntervalSchedule.Interval(duration, unit));
    }

    /**
     * Creates a cron schedule.
     *
     * @param cronExpressions   one or more cron expressions
     * @return                  the newly created cron schedule.
     * @throws                  org.elasticsearch.watcher.scheduler.schedule.CronSchedule.ValidationException if any of the given expression is invalid
     */
    public static CronSchedule cron(String... cronExpressions) {
        return new CronSchedule(cronExpressions);
    }

    /**
     * Creates an hourly schedule.
     *
     * @param minutes   the minutes within the hour that the schedule should trigger at. values must be
     *                  between 0 and 59 (inclusive).
     * @return          the newly created hourly schedule
     * @throws org.elasticsearch.watcher.WatcherSettingsException if any of the provided minutes are out of valid range
     */
    public static HourlySchedule hourly(int... minutes) {
        return new HourlySchedule(minutes);
    }

    /**
     * @return  A builder for an hourly schedule.
     */
    public static HourlySchedule.Builder hourly() {
        return HourlySchedule.builder();
    }

    /**
     * @return  A builder for a daily schedule.
     */
    public static DailySchedule.Builder daily() {
        return DailySchedule.builder();
    }

    /**
     * @return  A builder for a weekly schedule.
     */
    public static WeeklySchedule.Builder weekly() {
        return WeeklySchedule.builder();
    }

    /**
     * @return  A builder for a monthly schedule.
     */
    public static MonthlySchedule.Builder monthly() {
        return MonthlySchedule.builder();
    }
}
