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

package org.elasticsearch.watcher.condition.compare.array;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.clock.ClockMock;
import org.elasticsearch.watcher.support.clock.SystemClock;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

public class ArrayCompareConditionTests extends ESTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testOpEvalEQ() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 1), 1, ArrayCompareCondition.Op.EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.EQ), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 1, ArrayCompareCondition.Op.EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.EQ), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.EQ), is(false));
    }

    @Test
    public void testOpEvalNOT_EQ() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 1), 3, ArrayCompareCondition.Op.NOT_EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 1, ArrayCompareCondition.Op.NOT_EQ), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 1, ArrayCompareCondition.Op.NOT_EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 1), 1, ArrayCompareCondition.Op.NOT_EQ), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.NOT_EQ), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.NOT_EQ), is(false));
    }

    @Test
    public void testOpEvalGTE() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 1, ArrayCompareCondition.Op.GTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.GTE), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.GTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 4, ArrayCompareCondition.Op.GTE), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.GTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.GTE), is(false));
    }

    @Test
    public void testOpEvalGT() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 0, ArrayCompareCondition.Op.GT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 1, ArrayCompareCondition.Op.GT), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.GT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 4, ArrayCompareCondition.Op.GT), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.GT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.GT), is(false));
    }

    @Test
    public void testOpEvalLTE() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 3, ArrayCompareCondition.Op.LTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 0, ArrayCompareCondition.Op.LTE), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 3, ArrayCompareCondition.Op.LTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 0, ArrayCompareCondition.Op.LTE), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.LTE), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.LTE), is(false));
    }

    @Test
    public void testOpEvalLT() throws Exception {
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 4, ArrayCompareCondition.Op.LT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Arrays.<Object>asList(1, 3), 3, ArrayCompareCondition.Op.LT), is(false));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 2, ArrayCompareCondition.Op.LT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Arrays.<Object>asList(1, 3), 0, ArrayCompareCondition.Op.LT), is(false));
        assertThat(ArrayCompareCondition.Quantifier.ALL.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.LT), is(true));
        assertThat(ArrayCompareCondition.Quantifier.SOME.eval(Collections.emptyList(), 1, ArrayCompareCondition.Op.LT), is(false));
    }

    @Test
    public void testExecute() {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        int value = randomInt(10);
        int numberOfValues = randomIntBetween(0, 3);
        List<Object> values = new ArrayList<>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            values.add(randomInt(10));
        }
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        boolean met = quantifier.eval(values, value, op);

        logger.debug("op [{}]", op);
        logger.debug("value [{}]", value);
        logger.debug("numberOfValues [{}]", numberOfValues);
        logger.debug("values [{}]", values);
        logger.debug("quantifier [{}]", quantifier);
        logger.debug("met [{}]", met);

        ExecutableArrayCompareCondition condition = new ExecutableArrayCompareCondition(new ArrayCompareCondition("ctx.payload.value", "", op, value, quantifier), logger, SystemClock.INSTANCE);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", values));
        assertThat(condition.execute(ctx).met(), is(met));
    }

    @Test
    public void testExecutePath() {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        int value = randomInt(10);
        int numberOfValues = randomIntBetween(0, 3);
        List<Object> docCounts = new ArrayList<>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            docCounts.add(randomInt(10));
        }
        List<Object> values = new ArrayList<>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            Map<String, Object> map = new HashMap<>(1);
            map.put("doc_count", docCounts.get(i));
            values.add(map);
        }
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        boolean met = quantifier.eval(docCounts, value, op);

        logger.debug("op [{}]", op);
        logger.debug("value [{}]", value);
        logger.debug("numberOfValues [{}]", numberOfValues);
        logger.debug("values [{}]", values);
        logger.debug("quantifier [{}]", quantifier);
        logger.debug("met [{}]", met);

        ExecutableArrayCompareCondition condition = new ExecutableArrayCompareCondition(new ArrayCompareCondition("ctx.payload.value", "doc_count", op, value, quantifier), logger, SystemClock.INSTANCE);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", values));
        assertThat(condition.execute(ctx).met(), is(met));
    }

    @Test
    public void testExecuteDateMath() {
        ClockMock clock = new ClockMock();
        boolean met = randomBoolean();
        ArrayCompareCondition.Op op = met ? randomFrom(ArrayCompareCondition.Op.GT, ArrayCompareCondition.Op.GTE, ArrayCompareCondition.Op.NOT_EQ) : randomFrom(ArrayCompareCondition.Op.LT, ArrayCompareCondition.Op.LTE, ArrayCompareCondition.Op.EQ);
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.ALL, ArrayCompareCondition.Quantifier.SOME);
        String value = "<{now-1d}>";
        int numberOfValues = randomIntBetween(1, 10);
        List<Object> values = new ArrayList<>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            clock.fastForwardSeconds(1);
            values.add(clock.nowUTC());
        }

        ExecutableArrayCompareCondition condition = new ExecutableArrayCompareCondition(new ArrayCompareCondition("ctx.payload.value", "", op, value, quantifier), logger, clock);
        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.Simple("value", values));
        assertThat(condition.execute(ctx).met(), is(met));
    }

    @Test
    public void testParse() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        ArrayCompareCondition condition = factory.parseCondition("_id", parser);

        assertThat(condition, notNullValue());
        assertThat(condition.getArrayPath(), is("key1.key2"));
        assertThat(condition.getOp(), is(op));
        assertThat(condition.getValue(), is(value));
        assertThat(condition.getPath(), is("key3.key4"));
        assertThat(condition.getQuantifier(), is(quantifier));
    }

    @Test
    public void testParseContainsDuplicateOperator() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                        .endObject()
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("duplicate comparison operator");

        factory.parseCondition("_id", parser);
    }

    @Test
    public void testParseContainsUnknownOperator() throws IOException {
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject("unknown")
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("unknown comparison operator");

        factory.parseCondition("_id", parser);
    }

    @Test
    public void testParseContainsDuplicateValue() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("duplicate field \"value\"");

        factory.parseCondition("_id", parser);
    }

    @Test
    public void testParseContainsDuplicateQuantifier() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                            .field("quantifier", quantifier.id())
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("duplicate field \"quantifier\"");

        factory.parseCondition("_id", parser);
    }

    @Test
    public void testParseContainsUnknownQuantifier() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", "unknown")
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("unknown comparison quantifier");

        factory.parseCondition("_id", parser);
    }

    @Test
    public void testParseContainsUnexpectedFieldInComparisonOperator() throws IOException {
        ArrayCompareCondition.Op op = randomFrom(ArrayCompareCondition.Op.values());
        ArrayCompareCondition.Quantifier quantifier = randomFrom(ArrayCompareCondition.Quantifier.values());
        Object value = randomFrom("value", 1, null);
        ArrayCompareConditionFactory factory = new ArrayCompareConditionFactory(Settings.EMPTY, SystemClock.INSTANCE);
        XContentBuilder builder =
                jsonBuilder().startObject()
                    .startObject("key1.key2")
                        .field("path", "key3.key4")
                        .startObject(op.id())
                            .field("value", value)
                            .field("quantifier", quantifier.id())
                            .field("unexpected", "unexpected")
                        .endObject()
                    .endObject()
                .endObject();

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        expectedException.expect(ElasticsearchParseException.class);
        expectedException.expectMessage("expected a field indicating the comparison value or comparison quantifier");

        factory.parseCondition("_id", parser);
    }
}