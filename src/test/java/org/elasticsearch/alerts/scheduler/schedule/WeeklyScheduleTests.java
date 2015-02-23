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

package org.elasticsearch.alerts.scheduler.schedule;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.alerts.AlertsSettingsException;
import org.elasticsearch.alerts.scheduler.schedule.support.DayTimes;
import org.elasticsearch.alerts.scheduler.schedule.support.DayOfWeek;
import org.elasticsearch.alerts.scheduler.schedule.support.WeekTimes;
import org.elasticsearch.common.base.Joiner;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.primitives.Ints;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class WeeklyScheduleTests extends ScheduleTestCase {

    @Test
    public void test_Default() throws Exception {
        WeeklySchedule schedule = new WeeklySchedule();
        String[] crons = schedule.crons();
        assertThat(crons, arrayWithSize(1));
        assertThat(crons, arrayContaining("0 0 0 ? * MON"));
    }

    @Test @Repeat(iterations = 20)
    public void test_SingleTime() throws Exception {
        WeekTimes time = validWeekTime();
        WeeklySchedule schedule = new WeeklySchedule(time);
        String[] crons = schedule.crons();
        assertThat(crons, arrayWithSize(time.times().length));
        for (DayTimes dayTimes : time.times()) {
            assertThat(crons, hasItemInArray("0 " + Ints.join(",", dayTimes.minute()) + " " + Ints.join(",", dayTimes.hour()) + " ? * " + Joiner.on(",").join(time.days())));
        }
    }

    @Test @Repeat(iterations = 20)
    public void test_MultipleTimes() throws Exception {
        WeekTimes[] times = validWeekTimes();
        WeeklySchedule schedule = new WeeklySchedule(times);
        String[] crons = schedule.crons();
        int count = 0;
        for (int i = 0; i < times.length; i++) {
            count += times[i].times().length;
        }
        assertThat(crons, arrayWithSize(count));
        for (WeekTimes weekTimes : times) {
            for (DayTimes dayTimes : weekTimes.times()) {
                assertThat(crons, hasItemInArray("0 " + Ints.join(",", dayTimes.minute()) + " " + Ints.join(",", dayTimes.hour()) + " ? * " + Joiner.on(",").join(weekTimes.days())));
            }
        }
    }

    @Test
    public void testParser_Empty() throws Exception {
        XContentBuilder builder = jsonBuilder().startObject().endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        WeeklySchedule schedule = new WeeklySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.times().length, is(1));
        assertThat(schedule.times()[0], is(new WeekTimes(DayOfWeek.MONDAY, new DayTimes())));
    }

    @Test @Repeat(iterations = 20)
    public void testParser_SingleTime() throws Exception {
        DayTimes time = validDayTime();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("on", "mon")
                .startObject("at")
                .field("hour", time.hour())
                .field("minute", time.minute())
                .endObject()
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        WeeklySchedule schedule = new WeeklySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.times().length, is(1));
        assertThat(schedule.times()[0].days(), hasSize(1));
        assertThat(schedule.times()[0].days(), contains(DayOfWeek.MONDAY));
        assertThat(schedule.times()[0].times(), arrayWithSize(1));
        assertThat(schedule.times()[0].times(), hasItemInArray(time));
    }

    @Test(expected = AlertsSettingsException.class) @Repeat(iterations = 20)
    public void testParser_SingleTime_Invalid() throws Exception {
        HourAndMinute time = invalidDayTime();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("on", "mon")
                .startObject("at")
                .field("hour", time.hour)
                .field("minute", time.minute)
                .endObject()
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        new WeeklySchedule.Parser().parse(parser);
    }

    @Test @Repeat(iterations = 20)
    public void testParser_MultipleTimes() throws Exception {
        WeekTimes[] times = validWeekTimes();
        XContentBuilder builder = jsonBuilder().value(times);
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        WeeklySchedule schedule = new WeeklySchedule.Parser().parse(parser);
        assertThat(schedule, notNullValue());
        assertThat(schedule.times().length, is(times.length));
        for (int i = 0; i < times.length; i++) {
            assertThat(schedule.times(), hasItemInArray(times[i]));
        }
    }

    @Test(expected = AlertsSettingsException.class) @Repeat(iterations = 20)
    public void testParser_MultipleTimes_Objects_Invalid() throws Exception {
        HourAndMinute[] times = invalidDayTimes();
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("on", randomDaysOfWeek())
                .array("at", (Object[]) times)
                .endObject();
        BytesReference bytes = builder.bytes();
        XContentParser parser = JsonXContent.jsonXContent.createParser(bytes);
        parser.nextToken(); // advancing to the start object
        new WeeklySchedule.Parser().parse(parser);
    }
}
