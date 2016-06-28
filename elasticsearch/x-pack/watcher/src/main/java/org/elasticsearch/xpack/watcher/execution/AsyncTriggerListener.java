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

package org.elasticsearch.xpack.watcher.execution;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.watcher.trigger.TriggerEngine;
import org.elasticsearch.xpack.watcher.trigger.TriggerEvent;
import org.elasticsearch.xpack.watcher.trigger.TriggerService;

import java.util.stream.StreamSupport;

/**
 */
public class AsyncTriggerListener implements TriggerEngine.Listener {

    private final ESLogger logger;
    private final ExecutionService executionService;

    @Inject
    public AsyncTriggerListener(Settings settings, ExecutionService executionService, TriggerService triggerService) {
        this.logger = Loggers.getLogger(SyncTriggerListener.class, settings);
        this.executionService = executionService;
        triggerService.register(this);
    }

    @Override
    public void triggered(Iterable<TriggerEvent> events) {
        try {
            executionService.processEventsAsync(events);
        } catch (Exception e) {
            logger.error("failed to process triggered events [{}]", e,
                    (Object) StreamSupport.stream(events.spliterator(), false).toArray(size -> new TriggerEvent[size]));
        }

    }

}
