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

package org.elasticsearch.xpack.watcher.support;


import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.Version;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils.parseTimeValueSupportingFractional;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.xContentParser;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class WatcherDateTimeUtilsTests extends ESTestCase {
    public void testParseTimeValueNumeric() throws Exception {
        TimeValue value = new TimeValue(randomInt(100), randomFrom(TimeUnit.values()));
        long millis = value.getMillis();
        XContentBuilder xContentBuilder = jsonBuilder().startObject();
        if (randomBoolean() || millis == 0) { // 0 is special - no unit required
            xContentBuilder.field("value", millis);
        } else {
            xContentBuilder.field("value", Long.toString(millis));
        }
        XContentParser parser = xContentParser(xContentBuilder.endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        try {
            WatcherDateTimeUtils.parseTimeValue(parser, "test");
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), either(is("failed to parse time unit"))
                    .or(is("could not parse time value. expected either a string or a null value but found [VALUE_NUMBER] instead")));
        }
    }

    public void testParseTimeValueNumericNegative() throws Exception {
        TimeValue value = new TimeValue(randomIntBetween(1, 100), randomFrom(MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", -1 * value.getMillis()).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        try {
            WatcherDateTimeUtils.parseTimeValue(parser, "test");
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(),
                    is("could not parse time value. expected either a string or a null value but found [VALUE_NUMBER] instead"));
        }
    }

    public void testParseTimeValueString() throws Exception {
        int value = randomIntBetween(2, 200);
        Map<String, TimeValue> values = new HashMap<>();
        values.put(value + "s", TimeValue.timeValueSeconds(value));
        values.put(value + "m", TimeValue.timeValueMinutes(value));
        values.put(value + "h", TimeValue.timeValueHours(value));

        String key = randomFrom(values.keySet().toArray(new String[values.size()]));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", key).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, notNullValue());
        assertThat(parsed.millis(), is(values.get(key).millis()));
    }

    public void testParseTimeValueStringNegative() throws Exception {
        int value = -1 * randomIntBetween(2, 200);
        Map<String, TimeValue> values = new HashMap<>();
        values.put(value + "s", TimeValue.timeValueSeconds(value));
        values.put(value + "m", TimeValue.timeValueMinutes(value));
        values.put(value + "h", TimeValue.timeValueHours(value));

        String key = randomFrom(values.keySet().toArray(new String[values.size()]));

        XContentParser parser = xContentParser(jsonBuilder().startObject().field("value", key).endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        try {
            WatcherDateTimeUtils.parseTimeValue(parser, "test");
            fail("Expected ElasticsearchParseException");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), is("failed to parse time unit"));
        }
    }

    public void testParseTimeValueNull() throws Exception {
        XContentParser parser = xContentParser(jsonBuilder().startObject().nullField("value").endObject());
        parser.nextToken(); // start object
        parser.nextToken(); // field name
        parser.nextToken(); // value

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, nullValue());
    }

    public void testParseTimeValueWithFractional() {
        assertEquals("This function exists so 5.x can be compatible with 2.x indices. It should be removed with 6.x", 5,
                Version.CURRENT.major);

        // This code is lifted strait from 2.x's TimeValueTests.java
        assertEquals(new TimeValue(10, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("10 ms", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("10ms", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("10 MS", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("10MS", "test"));

        assertEquals(new TimeValue(10, TimeUnit.SECONDS), parseTimeValueSupportingFractional("10 s", "test"));
        assertEquals(new TimeValue(10, TimeUnit.SECONDS), parseTimeValueSupportingFractional("10s", "test"));
        assertEquals(new TimeValue(10, TimeUnit.SECONDS), parseTimeValueSupportingFractional("10 S", "test"));
        assertEquals(new TimeValue(10, TimeUnit.SECONDS), parseTimeValueSupportingFractional("10S", "test"));

        assertEquals(new TimeValue(100, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("0.1s", "test"));

        assertEquals(new TimeValue(10, TimeUnit.MINUTES), parseTimeValueSupportingFractional("10 m", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MINUTES), parseTimeValueSupportingFractional("10m", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MINUTES), parseTimeValueSupportingFractional("10 M", "test"));
        assertEquals(new TimeValue(10, TimeUnit.MINUTES), parseTimeValueSupportingFractional("10M", "test"));

        assertEquals(new TimeValue(10, TimeUnit.HOURS), parseTimeValueSupportingFractional("10 h", "test"));
        assertEquals(new TimeValue(10, TimeUnit.HOURS), parseTimeValueSupportingFractional("10h", "test"));
        assertEquals(new TimeValue(10, TimeUnit.HOURS), parseTimeValueSupportingFractional("10 H", "test"));
        assertEquals(new TimeValue(10, TimeUnit.HOURS), parseTimeValueSupportingFractional("10H", "test"));

        assertEquals(new TimeValue(10, TimeUnit.DAYS), parseTimeValueSupportingFractional("10 d", "test"));
        assertEquals(new TimeValue(10, TimeUnit.DAYS), parseTimeValueSupportingFractional("10d", "test"));
        assertEquals(new TimeValue(10, TimeUnit.DAYS), parseTimeValueSupportingFractional("10 D", "test"));
        assertEquals(new TimeValue(10, TimeUnit.DAYS), parseTimeValueSupportingFractional("10D", "test"));

        assertEquals(new TimeValue(70, TimeUnit.DAYS), parseTimeValueSupportingFractional("10 w", "test"));
        assertEquals(new TimeValue(70, TimeUnit.DAYS), parseTimeValueSupportingFractional("10w", "test"));
        assertEquals(new TimeValue(70, TimeUnit.DAYS), parseTimeValueSupportingFractional("10 W", "test"));
        assertEquals(new TimeValue(70, TimeUnit.DAYS), parseTimeValueSupportingFractional("10W", "test"));

        // Extra fractional tests just because that is the point
        assertEquals(new TimeValue(100, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("0.1s", "test"));
        assertEquals(new TimeValue(6, TimeUnit.SECONDS), parseTimeValueSupportingFractional("0.1m", "test"));
        assertEquals(new TimeValue(6, TimeUnit.MINUTES), parseTimeValueSupportingFractional("0.1h", "test"));
        assertEquals(new TimeValue(144, TimeUnit.MINUTES), parseTimeValueSupportingFractional("0.1d", "test"));
        assertEquals(new TimeValue(1008, TimeUnit.MINUTES), parseTimeValueSupportingFractional("0.1w", "test"));

        // And some crazy fractions just for fun
        assertEquals(new TimeValue(1700, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("1.7s", "test"));
        assertEquals(new TimeValue(162, TimeUnit.SECONDS), parseTimeValueSupportingFractional("2.7m", "test"));
        assertEquals(new TimeValue(5988, TimeUnit.MINUTES), parseTimeValueSupportingFractional("99.8h", "test"));
        assertEquals(new TimeValue(1057968, TimeUnit.SECONDS), parseTimeValueSupportingFractional("12.245d", "test"));
        assertEquals(new TimeValue(7258204799L, TimeUnit.MILLISECONDS), parseTimeValueSupportingFractional("12.001w", "test"));
    }
}
