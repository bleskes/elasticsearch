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

package org.elasticsearch.watcher.support;


import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.xContentParser;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class WatcherDateTimeUtilsTests extends ElasticsearchTestCase {

    @Test(expected = WatcherDateTimeUtils.ParseException.class)
    public void testParseTimeValue_Numeric() throws Exception {
        TimeValue value = new TimeValue(randomInt(100), randomFrom(TimeUnit.values()));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", value.getMillis()).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, notNullValue());
        assertThat(parsed.millis(), is(value.millis()));
    }

    @Test(expected = WatcherDateTimeUtils.ParseException.class)
    public void testParseTimeValue_Numeric_Negative() throws Exception {
        TimeValue value = new TimeValue(randomIntBetween(1, 100), randomFrom(MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", -1 * value.getMillis()).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        WatcherDateTimeUtils.parseTimeValue(parser, "test");
    }

    @Test
    public void testParseTimeValue_String() throws Exception {
        int value = randomIntBetween(2, 200);
        ImmutableMap<String, TimeValue> values = ImmutableMap.<String, TimeValue>builder()
                .put(value + "s", TimeValue.timeValueSeconds(value))
                .put(value + "m", TimeValue.timeValueMinutes(value))
                .put(value + "h", TimeValue.timeValueHours(value))
                .build();

        String key = randomFrom(values.keySet().toArray(new String[values.size()]));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", key).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, notNullValue());
        assertThat(parsed.millis(), is(values.get(key).millis()));
    }

    @Test(expected = WatcherDateTimeUtils.ParseException.class)
    public void testParseTimeValue_String_Negative() throws Exception {
        int value = -1 * randomIntBetween(2, 200);
        ImmutableMap<String, TimeValue> values = ImmutableMap.<String, TimeValue>builder()
                .put(value + "s", TimeValue.timeValueSeconds(value))
                .put(value + "m", TimeValue.timeValueMinutes(value))
                .put(value + "h", TimeValue.timeValueHours(value))
                .build();

        String key = randomFrom(values.keySet().toArray(new String[values.size()]));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", key).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        WatcherDateTimeUtils.parseTimeValue(parser, "test");
    }

    public void testParseTimeValue_Null() throws Exception {
        XContentParser parser = xContentParser(jsonBuilder().startObject().nullField("value").endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, nullValue());
    }
}
