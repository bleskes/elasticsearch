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

import org.elasticsearch.alerts.scheduler.schedule.CronSchedule;
import org.elasticsearch.alerts.scheduler.schedule.Schedule;
import org.elasticsearch.alerts.scheduler.schedule.ScheduleRegistry;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SchedulerModule extends AbstractModule {

    private final Map<String, Class<? extends Schedule.Parser>> parsers = new HashMap<>();

    public void registerSchedule(String type, Class<? extends Schedule.Parser> parser) {
        parsers.put(type, parser);
    }

    @Override
    protected void configure() {

        MapBinder<String, Schedule.Parser> mbinder = MapBinder.newMapBinder(binder(), String.class, Schedule.Parser.class);
        bind(CronSchedule.Parser.class).asEagerSingleton();
        mbinder.addBinding(CronSchedule.TYPE).to(CronSchedule.Parser.class);

        for (Map.Entry<String, Class<? extends Schedule.Parser>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(ScheduleRegistry.class).asEagerSingleton();
        bind(Scheduler.class).asEagerSingleton();
    }
}
