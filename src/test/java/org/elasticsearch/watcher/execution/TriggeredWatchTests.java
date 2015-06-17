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

import org.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.watcher.execution.*;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.watcher.watch.Watch;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 */
public class TriggeredWatchTests extends AbstractWatcherIntegrationTests {

    @Test
    public void testParser() throws Exception {
        Watch watch = WatcherTestUtils.createTestWatch("fired_test", scriptService(), watcherHttpClient(), noopEmailService(), logger);
        ScheduleTriggerEvent event = new ScheduleTriggerEvent(watch.id(), DateTime.now(DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC));
        Wid wid = new Wid("_record", randomLong(), DateTime.now(DateTimeZone.UTC));
        TriggeredWatch triggeredWatch = new TriggeredWatch(wid, event);
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        triggeredWatch.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        TriggeredWatch parsedTriggeredWatch = triggeredWatchParser().parse(triggeredWatch.id().value(), 0, jsonBuilder.bytes());

        XContentBuilder jsonBuilder2 = XContentFactory.jsonBuilder();
        parsedTriggeredWatch.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);

        assertThat(jsonBuilder.bytes().toUtf8(), equalTo(jsonBuilder2.bytes().toUtf8()));
    }

    private TriggeredWatch.Parser triggeredWatchParser() {
        return internalTestCluster().getInstance(TriggeredWatch.Parser.class);
    }


}
