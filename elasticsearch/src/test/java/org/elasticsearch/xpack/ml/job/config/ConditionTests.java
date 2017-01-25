/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import static org.hamcrest.Matchers.containsString;

public class ConditionTests extends AbstractSerializingTestCase<Condition> {

    public void testSetValues() {
        Condition cond = new Condition(Operator.EQ, "5");
        assertEquals(Operator.EQ, cond.getOperator());
        assertEquals("5", cond.getValue());
    }

    public void testHashCodeAndEquals() {
        Condition cond1 = new Condition(Operator.MATCH, "regex");
        Condition cond2 = new Condition(Operator.MATCH, "regex");

        assertEquals(cond1, cond2);
        assertEquals(cond1.hashCode(), cond2.hashCode());

        Condition cond3 = new Condition(Operator.EQ, "5");
        assertFalse(cond1.equals(cond3));
        assertFalse(cond1.hashCode() == cond3.hashCode());
    }

    @Override
    protected Condition createTestInstance() {
        Operator op = randomFrom(Operator.values());
        Condition condition;
        switch (op) {
        case EQ:
        case GT:
        case GTE:
        case LT:
        case LTE:
            condition = new Condition(op, Double.toString(randomDouble()));
            break;
        case MATCH:
            condition = new Condition(op, randomAsciiOfLengthBetween(1, 20));
            break;
        default:
            throw new AssertionError("Unknown operator selected: " + op.getName());
        }
        return condition;
    }

    @Override
    protected Reader<Condition> instanceReader() {
        return Condition::new;
    }

    @Override
    protected Condition parseInstance(XContentParser parser) {
        return Condition.PARSER.apply(parser, null);
    }

    public void testVerifyArgsNumericArgs() {
        new Condition(Operator.LTE, "100");
        new Condition(Operator.GT, "10.0");
    }

    public void testVerify_GivenEmptyValue() {
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> new Condition(Operator.LT, ""));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, ""), e.getMessage());
    }

    public void testVerify_GivenInvalidRegex() {
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> new Condition(Operator.MATCH, "[*"));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_REGEX, "[*"), e.getMessage());
    }

    public void testVerify_GivenNullRegex() {
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> new Condition(Operator.MATCH, null));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NULL, "[*"), e.getMessage());
    }
}
