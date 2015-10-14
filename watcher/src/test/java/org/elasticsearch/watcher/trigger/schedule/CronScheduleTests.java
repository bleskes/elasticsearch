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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class CronScheduleTests extends ScheduleTestCase {
    public void testInvalid() throws Exception {
        try {
            new CronSchedule("0 * * *");
            fail("expecting a validation error to be thrown when creating a cron schedule with invalid cron expression");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid cron expression [0 * * *]"));
        }
    }

    public void testParseSingle() throws Exception {
        XContentBuilder builder = jsonBuilder().value("0 0/5 * * * ?");
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        CronSchedule schedule = new CronSchedule.Parser().parse(parser);
        assertThat(schedule.crons(), arrayWithSize(1));
        assertThat(schedule.crons()[0].expression(), is("0 0/5 * * * ?"));
    }

    public void testParseMultiple() throws Exception {
        XContentBuilder builder = jsonBuilder().value(new String[] {
                "0 0/1 * * * ?",
                "0 0/2 * * * ?",
                "0 0/3 * * * ?"
        });
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        CronSchedule schedule = new CronSchedule.Parser().parse(parser);
        String[] crons = expressions(schedule);
        assertThat(crons, arrayWithSize(3));
        assertThat(crons, hasItemInArray("0 0/1 * * * ?"));
        assertThat(crons, hasItemInArray("0 0/2 * * * ?"));
        assertThat(crons, hasItemInArray("0 0/3 * * * ?"));
    }

    public void testParseInvalidBadExpression() throws Exception {
        XContentBuilder builder = jsonBuilder().value("0 0/5 * * ?");
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        try {
            new CronSchedule.Parser().parse(parser);
            fail("expected cron parsing to fail when using invalid cron expression");
        } catch (ElasticsearchParseException pe) {
            // expected
            assertThat(pe.getCause(), instanceOf(IllegalArgumentException.class));
        }
    }

    public void testParseInvalidEmpty() throws Exception {
        XContentBuilder builder = jsonBuilder();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        try {
            new CronSchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [cron] schedule. expected either a cron string value or an array of cron string values, but found [null]"));
        }
    }

    public void testParseInvalidObject() throws Exception {
        XContentBuilder builder = jsonBuilder().startObject().endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        try {
            new CronSchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [cron] schedule. expected either a cron string value or an array of cron string values, but found [START_OBJECT]"));
        }
    }

    public void testParseInvalidEmptyArray() throws Exception {
        XContentBuilder builder = jsonBuilder().value(new String[0]);
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken();
        try {
            new CronSchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [cron] schedule. no cron expression found in cron array"));
        }
    }
}
