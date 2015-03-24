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

package org.elasticsearch.watcher.scheduler;

import org.elasticsearch.watcher.scheduler.schedule.Schedule;
import org.elasticsearch.common.joda.time.DateTime;

import java.util.Collection;

/**
 *
 */
public interface Scheduler {

    /**
     * Starts the scheduler and schedules the specified jobs before returning.
     */
    void start(Collection<? extends Job> jobs);

    /**
     * Stops the scheduler.
     */
    void stop();

    /**
     * Adds and schedules the give job
     */
    void add(Job job);

    /**
     * Removes the scheduled job that is associated with the given name
     */
    boolean remove(String jobName);

    void addListener(Listener listener);

    public static interface Listener {

        void fire(String jobName, DateTime scheduledFireTime, DateTime fireTime);

    }

    public static interface Job {

        String name();

        Schedule schedule();

    }
}
