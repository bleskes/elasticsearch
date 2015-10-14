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
import org.elasticsearch.watcher.support.Strings;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.support.Integers.asIterable;
import static org.elasticsearch.watcher.support.Integers.contains;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class HourlyScheduleTests extends ScheduleTestCase {
    public void testDefault() throws Exception {
        HourlySchedule schedule = new HourlySchedule();
        String[] crons = expressions(schedule);
        assertThat(crons, arrayWithSize(1));
        assertThat(crons, arrayContaining("0 0 * * * ?"));
    }

    public void testSingleMinute() throws Exception {
        int minute = validMinute();
        HourlySchedule schedule = new HourlySchedule(minute);
        String[] crons = expressions(schedule);
        assertThat(crons, arrayWithSize(1));
        assertThat(crons, arrayContaining("0 " + minute + " * * * ?"));
    }

    public void testSingleMinuteInvalid() throws Exception {
        try {
            new HourlySchedule(invalidMinute());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("invalid hourly minute"));
            assertThat(e.getMessage(), containsString("minute must be between 0 and 59 incl."));
        }
    }

    public void testMultipleMinutes() throws Exception {
        int[] minutes = validMinutes();
        String minutesStr = Strings.join(",", minutes);
        HourlySchedule schedule = new HourlySchedule(minutes);
        String[] crons = expressions(schedule);
        assertThat(crons, arrayWithSize(1));
        assertThat(crons, arrayContaining("0 " + minutesStr + " * * * ?"));
    }

    public void testMultipleMinutesInvalid() throws Exception {
        int[] minutes = invalidMinutes();
        try {
            new HourlySchedule(minutes);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("invalid hourly minute"));
            assertThat(e.getMessage(), containsString("minute must be between 0 and 59 incl."));
        }
    }

    public void testParserEmpty() throws Exception {
        XContentBuilder builder = jsonBuilder().startObject().endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        HourlySchedule schedule = new HourlySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.minutes().length, is(1));
        assertThat(schedule.minutes()[0], is(0));
    }

    public void testParserSingleMinuteNumber() throws Exception {
        int minute = validMinute();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", minute)
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        HourlySchedule schedule = new HourlySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.minutes().length, is(1));
        assertThat(schedule.minutes()[0], is(minute));
    }

    public void testParserSingleMinuteNumberInvalid() throws Exception {
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", invalidMinute())
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        try {
            new HourlySchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [hourly] schedule. invalid value for [minute]"));
        }
    }

    public void testParserSingleMinuteString() throws Exception {
        int minute = validMinute();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", String.valueOf(minute))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        HourlySchedule schedule = new HourlySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.minutes().length, is(1));
        assertThat(schedule.minutes()[0], is(minute));
    }

    public void testParserSingleMinuteStringInvalid() throws Exception {
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", String.valueOf(invalidMinute()))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        try {
            new HourlySchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [hourly] schedule. invalid value for [minute]"));
        }
    }

    public void testParserMultipleMinutesNumbers() throws Exception {
        int[] minutes = validMinutes();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", asIterable(minutes))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        HourlySchedule schedule = new HourlySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.minutes().length, is(minutes.length));
        for (int i = 0; i < minutes.length; i++) {
            assertThat(contains(schedule.minutes(), minutes[i]), is(true));
        }
    }

    public void testParserMultipleMinutesNumbersInvalid() throws Exception {
        int[] minutes = invalidMinutes();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", asIterable(minutes))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        try {
            new HourlySchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [hourly] schedule. invalid value for [minute]"));
        }
    }

    public void testParserMultipleMinutesStrings() throws Exception {
        int[] minutes = validMinutes();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", Arrays.stream(minutes).mapToObj(p -> Integer.toString(p)).collect(Collectors.toList()))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        HourlySchedule schedule = new HourlySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.minutes().length, is(minutes.length));
        for (int i = 0; i < minutes.length; i++) {
            assertThat(contains(schedule.minutes(), minutes[i]), is(true));
        }
    }

    public void testParserMultipleMinutesStringsInvalid() throws Exception {
        int[] minutes = invalidMinutes();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("minute", Arrays.stream(minutes).mapToObj(p -> Integer.toString(p)).collect(Collectors.toList()))
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        try {
            new HourlySchedule.Parser().parse(parser);
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("could not parse [hourly] schedule. invalid value for [minute]"));
        }
    }
}
