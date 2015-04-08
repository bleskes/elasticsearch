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

package org.elasticsearch.watcher.trigger.schedule;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.trigger.TriggerEngine;
import org.elasticsearch.watcher.trigger.schedule.engine.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class ScheduleModule extends AbstractModule {

    private final Map<String, Class<? extends Schedule.Parser>> parsers = new HashMap<>();

    public ScheduleModule() {
        registerScheduleParser(CronSchedule.TYPE, CronSchedule.Parser.class);
        registerScheduleParser(DailySchedule.TYPE, DailySchedule.Parser.class);
        registerScheduleParser(HourlySchedule.TYPE, HourlySchedule.Parser.class);
        registerScheduleParser(IntervalSchedule.TYPE, IntervalSchedule.Parser.class);
        registerScheduleParser(MonthlySchedule.TYPE, MonthlySchedule.Parser.class);
        registerScheduleParser(WeeklySchedule.TYPE, WeeklySchedule.Parser.class);
        registerScheduleParser(YearlySchedule.TYPE, YearlySchedule.Parser.class);
    }

    public static Class<? extends TriggerEngine> triggerEngineType(Settings nodeSettings) {
        Engine engine = Engine.resolve(nodeSettings);
        Loggers.getLogger(ScheduleModule.class, nodeSettings).info("using [{}] schedule trigger engine", engine.name().toLowerCase(Locale.ROOT));
        return engine.engineType();
    }

    public void registerScheduleParser(String parserType, Class<? extends Schedule.Parser> parserClass) {
        parsers.put(parserType, parserClass);
    }

    @Override
    protected void configure() {

        MapBinder<String, Schedule.Parser> mbinder = MapBinder.newMapBinder(binder(), String.class, Schedule.Parser.class);
        for (Map.Entry<String, Class<? extends Schedule.Parser>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(ScheduleRegistry.class).asEagerSingleton();
    }

    public static Settings additionalSettings(Settings nodeSettings) {
        Engine engine = Engine.resolve(nodeSettings);
        return engine.additionalSettings(nodeSettings);
    }

    public enum Engine {

        SCHEDULER() {
            @Override
            protected Class<? extends TriggerEngine> engineType() {
                return SchedulerScheduleTriggerEngine.class;
            }

            @Override
            protected Settings additionalSettings(Settings nodeSettings) {
                return SchedulerScheduleTriggerEngine.additionalSettings(nodeSettings);
            }
        },

        HASHWHEEL() {
            @Override
            protected Class<? extends TriggerEngine> engineType() {
                return HashWheelScheduleTriggerEngine.class;
            }

            @Override
            protected Settings additionalSettings(Settings nodeSettings) {
                return HashWheelScheduleTriggerEngine.additionalSettings(nodeSettings);
            }
        },

        QUARTZ() {
            @Override
            protected Class<? extends TriggerEngine> engineType() {
                return QuartzScheduleTriggerEngine.class;
            }

            @Override
            protected Settings additionalSettings(Settings nodeSettings) {
                return QuartzScheduleTriggerEngine.additionalSettings(nodeSettings);
            }
        },

        TIMER() {
            @Override
            protected Class<? extends TriggerEngine> engineType() {
                return TimerTickerScheduleTriggerEngine.class;
            }

            @Override
            protected Settings additionalSettings(Settings nodeSettings) {
                return TimerTickerScheduleTriggerEngine.additionalSettings(nodeSettings);
            }
        },

        SIMPLE() {
            @Override
            protected Class<? extends TriggerEngine> engineType() {
                return SimpleTickerScheduleTriggerEngine.class;
            }

            @Override
            protected Settings additionalSettings(Settings nodeSettings) {
                return SimpleTickerScheduleTriggerEngine.additionalSettings(nodeSettings);
            }
        };

        protected abstract Class<? extends TriggerEngine> engineType();

        protected abstract Settings additionalSettings(Settings nodeSettings);

        public static Engine resolve(Settings settings) {
            String engine = settings.getComponentSettings(ScheduleModule.class).get("engine", "scheduler");
            switch (engine.toLowerCase(Locale.ROOT)) {
                case "quartz"    : return QUARTZ;
                case "timer"     : return TIMER;
                case "simple"    : return SIMPLE;
                case "hashwheel" : return HASHWHEEL;
                case "scheduler" : return SCHEDULER;
                default:
                    return SCHEDULER;
            }
        }
    }

}
