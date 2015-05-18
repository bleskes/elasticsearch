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

package org.elasticsearch.watcher.condition.compare;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.condition.compare.CompareCondition.Op;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.clock.ClockMock;
import org.elasticsearch.watcher.support.clock.SystemClock;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.Test;

import java.util.Locale;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 */
@Seed("9E70A915296AA3F2:FAA20587D7DCA86B")
public class CompareConditionTests extends ElasticsearchTestCase {

    @Test
    public void testOpEval_EQ() throws Exception {
        assertThat(Op.EQ.eval(null, null), is(true));
        assertThat(Op.EQ.eval(4, 3.0), is(false));
        assertThat(Op.EQ.eval(3, 3.0), is(true));
        assertThat(Op.EQ.eval(2, new Float(3.0)), is(false));
        assertThat(Op.EQ.eval(3, null), is(false));
        assertThat(Op.EQ.eval(2, "2"), is(true));     // comparing as strings
        assertThat(Op.EQ.eval(3, "4"), is(false));    // comparing as strings
        assertThat(Op.EQ.eval(3, "a"), is(false));    // comparing as strings
        assertThat(Op.EQ.eval("3", 3), is(true));     // comparing as numbers
        assertThat(Op.EQ.eval("a", "aa"), is(false));
        assertThat(Op.EQ.eval("a", "a"), is(true));
        assertThat(Op.EQ.eval("aa", "ab"), is(false));
        assertThat(Op.EQ.eval(ImmutableMap.of("k", "v"), ImmutableMap.of("k", "v")), is(true));
        assertThat(Op.EQ.eval(ImmutableMap.of("k", "v"), ImmutableMap.of("k1", "v1")), is(false));
        assertThat(Op.EQ.eval(ImmutableList.of("k", "v"), ImmutableList.of("k", "v")), is(true));
        assertThat(Op.EQ.eval(ImmutableList.of("k", "v"), ImmutableList.of("k1", "v1")), is(false));
    }

    @Test
    public void testOpEval_NOT_EQ() throws Exception {
        assertThat(Op.NOT_EQ.eval(null, null), is(false));
        assertThat(Op.NOT_EQ.eval(4, 3.0), is(true));
        assertThat(Op.NOT_EQ.eval(3, 3.0), is(false));
        assertThat(Op.NOT_EQ.eval(2, new Float(3.0)), is(true));
        assertThat(Op.NOT_EQ.eval(3, null), is(true));
        assertThat(Op.NOT_EQ.eval(2, "2"), is(false));     // comparing as strings
        assertThat(Op.NOT_EQ.eval(3, "4"), is(true));    // comparing as strings
        assertThat(Op.NOT_EQ.eval(3, "a"), is(true));    // comparing as strings
        assertThat(Op.NOT_EQ.eval("3", 3), is(false));     // comparing as numbers
        assertThat(Op.NOT_EQ.eval("a", "aa"), is(true));
        assertThat(Op.NOT_EQ.eval("a", "a"), is(false));
        assertThat(Op.NOT_EQ.eval("aa", "ab"), is(true));
        assertThat(Op.NOT_EQ.eval(ImmutableMap.of("k", "v"), ImmutableMap.of("k", "v")), is(false));
        assertThat(Op.NOT_EQ.eval(ImmutableMap.of("k", "v"), ImmutableMap.of("k1", "v1")), is(true));
        assertThat(Op.NOT_EQ.eval(ImmutableList.of("k", "v"), ImmutableList.of("k", "v")), is(false));
        assertThat(Op.NOT_EQ.eval(ImmutableList.of("k", "v"), ImmutableList.of("k1", "v1")), is(true));
    }

    @Test
    public void testOpEval_GTE() throws Exception {
        assertThat(Op.GTE.eval(4, 3.0), is(true));
        assertThat(Op.GTE.eval(3, 3.0), is(true));
        assertThat(Op.GTE.eval(2, new Float(3.0)), is(false));
        assertThat(Op.GTE.eval(3, null), is(false));
        assertThat(Op.GTE.eval(3, "2"), is(true));     // comparing as strings
        assertThat(Op.GTE.eval(3, "4"), is(false));    // comparing as strings
        assertThat(Op.GTE.eval(3, "a"), is(false));    // comparing as strings
        assertThat(Op.GTE.eval("4", 3), is(true));     // comparing as numbers
        assertThat(Op.GTE.eval("a", "aa"), is(false));
        assertThat(Op.GTE.eval("a", "a"), is(true));
        assertThat(Op.GTE.eval("aa", "ab"), is(false));
    }

    @Test
    public void testOpEval_GT() throws Exception {
        assertThat(Op.GT.eval(4, 3.0), is(true));
        assertThat(Op.GT.eval(3, 3.0), is(false));
        assertThat(Op.GT.eval(2, new Float(3.0)), is(false));
        assertThat(Op.GT.eval(3, null), is(false));
        assertThat(Op.GT.eval(3, "2"), is(true));     // comparing as strings
        assertThat(Op.GT.eval(3, "4"), is(false));    // comparing as strings
        assertThat(Op.GT.eval(3, "a"), is(false));    // comparing as strings
        assertThat(Op.GT.eval("4", 3), is(true));     // comparing as numbers
        assertThat(Op.GT.eval("a", "aa"), is(false));
        assertThat(Op.GT.eval("a", "a"), is(false));
        assertThat(Op.GT.eval("aa", "ab"), is(false));
    }

    @Test
    public void testOpEval_LTE() throws Exception {
        assertThat(Op.LTE.eval(4, 3.0), is(false));
        assertThat(Op.LTE.eval(3, 3.0), is(true));
        assertThat(Op.LTE.eval(2, new Float(3.0)), is(true));
        assertThat(Op.LTE.eval(3, null), is(false));
        assertThat(Op.LTE.eval(3, "2"), is(false));     // comparing as strings
        assertThat(Op.LTE.eval(3, "4"), is(true));    // comparing as strings
        assertThat(Op.LTE.eval(3, "a"), is(true));    // comparing as strings
        assertThat(Op.LTE.eval("4", 3), is(false));     // comparing as numbers
        assertThat(Op.LTE.eval("a", "aa"), is(true));
        assertThat(Op.LTE.eval("a", "a"), is(true));
        assertThat(Op.LTE.eval("aa", "ab"), is(true));
    }

    @Test
    public void testOpEval_LT() throws Exception {
        assertThat(Op.LT.eval(4, 3.0), is(false));
        assertThat(Op.LT.eval(3, 3.0), is(false));
        assertThat(Op.LT.eval(2, new Float(3.0)), is(true));
        assertThat(Op.LT.eval(3, null), is(false));
        assertThat(Op.LT.eval(3, "2"), is(false));     // comparing as strings
        assertThat(Op.LT.eval(3, "4"), is(true));    // comparing as strings
        assertThat(Op.LT.eval(3, "a"), is(true));    // comparing as strings
        assertThat(Op.LT.eval("4", 3), is(false));     // comparing as numbers
        assertThat(Op.LT.eval("a", "aa"), is(true));
        assertThat(Op.LT.eval("a", "a"), is(false));
        assertThat(Op.LT.eval("aa", "ab"), is(true));
    }

    @Test @Repeat(iterations = 10)
    public void testExecute() throws Exception {
        Op op = randomFrom(Op.values());
        int value = randomInt(10);
        int payloadValue = randomInt(10);
        boolean met = op.eval(payloadValue, value);

        ExecutableCompareCondition condition = new ExecutableCompareCondition(new CompareCondition("ctx.payload.value", op, value), logger, SystemClock.INSTANCE);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", payloadValue));
        assertThat(condition.execute(ctx).met(), is(met));
    }

    @Test @Repeat(iterations = 10)
    public void testExecute_DateMath() throws Exception {
        ClockMock clock = new ClockMock();
        boolean met = randomBoolean();
        Op op = met ? randomFrom(Op.GT, Op.GTE, Op.NOT_EQ) : randomFrom(Op.LT, Op.LTE, Op.EQ);
        String value = "<{now-1d}>";
        DateTime payloadValue = clock.now();

        ExecutableCompareCondition condition = new ExecutableCompareCondition(new CompareCondition("ctx.payload.value", op, value), logger, clock);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", payloadValue));
        assertThat(condition.execute(ctx).met(), is(met));
    }

    @Test @Repeat(iterations = 5)
    public void testExecute_Path() throws Exception {
        ClockMock clock = new ClockMock();
        boolean met = randomBoolean();
        Op op = met ? Op.EQ : Op.NOT_EQ;
        String value = "{{ctx.payload.value}}";
        Object payloadValue = new Object();

        ExecutableCompareCondition condition = new ExecutableCompareCondition(new CompareCondition("ctx.payload.value", op, value), logger, clock);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", payloadValue));
        assertThat(condition.execute(ctx).met(), is(met));
    }


    @Test @Repeat(iterations =  10)
    public void testParse_Valid() throws Exception {
        Op op = randomFrom(Op.values());
        Object value = randomFrom("value", 1, null);
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.startObject("key1.key2");
        builder.field(op.name().toLowerCase(Locale.ROOT), value);
        builder.endObject();
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        CompareCondition condition = factory.parseCondition("_id", parser);

        assertThat(condition, notNullValue());
        assertThat(condition.getPath(), is("key1.key2"));
        assertThat(condition.getOp(), is(op));
        assertThat(condition.getValue(), is(value));
    }

    @Test(expected = CompareConditionException.class)
    public void testParse_InValid_NoOperationBody() throws Exception {
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.startObject("key1.key2");
        builder.endObject();
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        factory.parseCondition("_id", parser);
    }

    @Test(expected = CompareConditionException.class)
    public void testParse_InValid_UnknownOp() throws Exception {
        Object value = randomFrom("value", 1, null);
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.startObject("key1.key2");
        builder.field("foobar", value);
        builder.endObject();
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        factory.parseCondition("_id", parser);
    }

    @Test(expected = CompareConditionException.class) @Repeat(iterations = 10)
    public void testParse_InValid_WrongValueForOp() throws Exception {
        Object value = randomFrom(ImmutableList.of("1", "2"), ImmutableMap.of("key", "value"));
        String op = randomFrom("lt", "lte", "gt", "gte");
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.startObject("key1.key2");
        builder.field(op, value);
        builder.endObject();
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        factory.parseCondition("_id", parser);
    }


    @Test @Repeat(iterations = 10)
    public void testParse_Result_Valid() throws Exception {
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);

        boolean met = randomBoolean();
        Object resolvedValue = randomFrom("1", 5, null, ImmutableList.of("1", "2"), ImmutableMap.of("key", "value"));

        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("met", met);
        builder.field("resolved_value", resolvedValue);
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        CompareCondition.Result result = factory.parseResult("_id", parser);
        assertThat(result, notNullValue());
        assertThat(result.met(), is(met));
        assertThat(result.getResolveValue(), is(resolvedValue));
    }

    @Test(expected = CompareConditionException.class)
    public void testParse_Result_Invalid_MissingResolvedValue() throws Exception {
        CompareConditionFactory factory = new CompareConditionFactory(ImmutableSettings.EMPTY, SystemClock.INSTANCE);

        boolean met = randomBoolean();

        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("met", met);
        builder.endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        factory.parseResult("_id", parser);
    }
}
