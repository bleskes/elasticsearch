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

package org.elasticsearch.watcher.test.integration;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.watcher.execution.ActionExecutionMode;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.junit.Test;

/**
 *
 */
public class WatchExecuteTests  extends AbstractWatcherIntegrationTests {


    @Test(expected = ActionRequestValidationException.class)
    public void testExecute_InvalidWatchId() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        watcherClient().prepareExecuteWatch("id with whitespaces")
                .setTriggerEvent(new ScheduleTriggerEvent(now, now))
                .get();
    }

    @Test(expected = ActionRequestValidationException.class)
    public void testExecute_InvalidActionId() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        watcherClient().prepareExecuteWatch("_id")
                .setTriggerEvent(new ScheduleTriggerEvent(now, now))
                .setActionMode("id with whitespaces", randomFrom(ActionExecutionMode.values()))
                .get();
    }
}
