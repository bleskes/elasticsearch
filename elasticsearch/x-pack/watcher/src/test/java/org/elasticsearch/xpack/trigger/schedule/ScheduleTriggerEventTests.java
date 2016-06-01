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

package org.elasticsearch.xpack.trigger.schedule;


import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.support.clock.SystemClock;

import static org.hamcrest.Matchers.is;

/**
 */
public class ScheduleTriggerEventTests extends ESTestCase {
    public void testParserRandomDateMath() throws Exception {
        String triggeredTime = randomFrom("now", "now+5m", "2015-05-07T22:24:41.254Z", "2015-05-07T22:24:41.254Z||-5m");
        String scheduledTime = randomFrom("now", "now-5m", "2015-05-07T22:24:41.254Z", "2015-05-07T22:24:41.254Z||+5h");
        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.field(ScheduleTriggerEvent.Field.SCHEDULED_TIME.getPreferredName(), scheduledTime);
        jsonBuilder.field(ScheduleTriggerEvent.Field.TRIGGERED_TIME.getPreferredName(), triggeredTime);
        jsonBuilder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        parser.nextToken();

        ScheduleTriggerEvent scheduleTriggerEvent = ScheduleTriggerEvent.parse(parser, "_id", "_context", SystemClock.INSTANCE);
        assertThat(scheduleTriggerEvent.scheduledTime().isAfter(0), is(true));
        assertThat(scheduleTriggerEvent.triggeredTime().isAfter(0), is(true));
    }
}
