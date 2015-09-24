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


import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.xContentParser;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class WatcherDateTimeUtilsTests extends ESTestCase {

    @Test(expected = ElasticsearchParseException.class)
    public void testParseTimeValue_Numeric() throws Exception {
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

        TimeValue parsed = WatcherDateTimeUtils.parseTimeValue(parser, "test");
        assertThat(parsed, notNullValue());
        assertThat(parsed.millis(), is(value.millis()));
    }

    @Test(expected = ElasticsearchParseException.class)
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

    @Test(expected = ElasticsearchParseException.class)
    public void testParseTimeValue_String_Negative() throws Exception {
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
