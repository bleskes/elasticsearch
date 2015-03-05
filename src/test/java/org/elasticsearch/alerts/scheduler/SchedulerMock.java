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

package org.elasticsearch.alerts.scheduler;

import org.elasticsearch.alerts.support.clock.Clock;
import org.elasticsearch.alerts.support.clock.ClockMock;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A mock scheduler to help with unit testing. Provide {@link SchedulerMock#fire} method to manually trigger
 * jobs.
 */
public class SchedulerMock implements Scheduler {

    private final ESLogger logger;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<>();
    private final Clock clock;

    @Inject
    public SchedulerMock(Settings settings, Clock clock) {
        this.logger = Loggers.getLogger(SchedulerMock.class, settings);
        this.clock = clock;
    }

    @Override
    public void start(Collection<? extends Job> jobs) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void add(Job job) {
        jobs.put(job.name(), job);
    }

    @Override
    public boolean remove(String jobName) {
        return jobs.remove(jobName) != null;
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void fire(String jobName) {
        fire(jobName, 1, null);
    }

    public void fire(String jobName, int times) {
        fire(jobName, times, null);
    }

    public void fire(String jobName, int times, TimeValue interval) {
        for (int i = 0; i < times; i++) {
            DateTime now = clock.now();
            logger.debug("firing [" + jobName + "] at [" + now + "]");
            for (Listener listener : listeners) {
                listener.fire(jobName, now, now);
            }
            if (clock instanceof ClockMock) {
                ((ClockMock) clock).fastForward(interval == null ? TimeValue.timeValueMillis(10) : interval);
            } else {
                if (interval != null) {
                    try {
                        Thread.sleep(interval.millis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
