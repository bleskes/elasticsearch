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

package org.elasticsearch.watcher.trigger.schedule.engine;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.support.clock.SystemClock;
import org.elasticsearch.watcher.trigger.TriggerEngine;
import org.elasticsearch.watcher.trigger.schedule.ScheduleRegistry;

import static org.mockito.Mockito.mock;

/**
 */
public class TickerScheduleEngineTests extends BaseTriggerEngineTestCase {

    @Override
    protected TriggerEngine createEngine() {
        return new TickerScheduleTriggerEngine(Settings.EMPTY, mock(ScheduleRegistry.class), SystemClock.INSTANCE);
    }
}
