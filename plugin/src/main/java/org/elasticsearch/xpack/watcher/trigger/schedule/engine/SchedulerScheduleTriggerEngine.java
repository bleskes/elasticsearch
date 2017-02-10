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

package org.elasticsearch.xpack.watcher.trigger.schedule.engine;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.scheduler.SchedulerEngine;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.trigger.TriggerEvent;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleRegistry;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTrigger;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTriggerEngine;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SchedulerScheduleTriggerEngine extends ScheduleTriggerEngine {

    private final SchedulerEngine schedulerEngine;

    @Inject
    public SchedulerScheduleTriggerEngine(Settings settings, ScheduleRegistry scheduleRegistry, Clock clock) {
        super(settings, scheduleRegistry, clock);
        this.schedulerEngine = new SchedulerEngine(clock);
        this.schedulerEngine.register(event ->
                notifyListeners(event.getJobName(), event.getTriggeredTime(), event.getScheduledTime()));
    }

    @Override
    public void start(Collection<Job> jobs) {
        logger.debug("starting schedule engine...");
        final List<SchedulerEngine.Job> schedulerJobs = new ArrayList<>();
        jobs.stream()
                .filter(job -> job.trigger() instanceof ScheduleTrigger)
                .forEach(job -> {
                    ScheduleTrigger trigger = (ScheduleTrigger) job.trigger();
                    schedulerJobs.add(new SchedulerEngine.Job(job.id(), trigger.getSchedule()));
                });
        schedulerEngine.start(schedulerJobs);
        logger.debug("schedule engine started at [{}]", clock.nowUTC());
    }

    @Override
    public void stop() {
        logger.debug("stopping schedule engine...");
        schedulerEngine.stop();
        logger.debug("schedule engine stopped");
    }

    @Override
    public void add(Job job) {
        assert job.trigger() instanceof ScheduleTrigger;
        ScheduleTrigger trigger = (ScheduleTrigger) job.trigger();
        schedulerEngine.add(new SchedulerEngine.Job(job.id(), trigger.getSchedule()));
    }

    @Override
    public boolean remove(String jobId) {
        return schedulerEngine.remove(jobId);
    }

    protected void notifyListeners(String name, long triggeredTime, long scheduledTime) {
        logger.trace("triggered job [{}] at [{}] (scheduled time was [{}])", name, new DateTime(triggeredTime, DateTimeZone.UTC),
                new DateTime(scheduledTime, DateTimeZone.UTC));
        final ScheduleTriggerEvent event = new ScheduleTriggerEvent(name, new DateTime(triggeredTime, DateTimeZone.UTC),
                new DateTime(scheduledTime, DateTimeZone.UTC));
        for (Listener listener : listeners) {
            listener.triggered(Collections.<TriggerEvent>singletonList(event));
        }
    }
}
