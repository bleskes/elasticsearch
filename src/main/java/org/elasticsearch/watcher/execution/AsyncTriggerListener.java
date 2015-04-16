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

package org.elasticsearch.watcher.execution;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.watcher.trigger.TriggerEngine;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.TriggerService;

/**
 */
public class AsyncTriggerListener implements TriggerEngine.Listener {

    private final ExecutionService executionService;

    @Inject
    public AsyncTriggerListener(ExecutionService executionService, TriggerService triggerService) {
        this.executionService = executionService;
        triggerService.register(this);
    }

    @Override
    public void triggered(Iterable<TriggerEvent> events) {
        executionService.processEventsAsync(events);
    }

}
