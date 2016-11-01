/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class RuleConditionTests extends AbstractSerializingTestCase<RuleCondition> {

    @Override
    protected RuleCondition createTestInstance() {
        Condition condition = null;
        String fieldName = null;
        String valueList = null;
        String fieldValue = null;
        RuleConditionType r = randomFrom(RuleConditionType.values());
        switch (r) {
        case CATEGORICAL:
            valueList = randomAsciiOfLengthBetween(1, 20);
            if (randomBoolean()) {
                fieldName = randomAsciiOfLengthBetween(1, 20);
            }
            break;
        default:
            // no need to randomize, it is properly randomily tested in
            // ConditionTest
            condition = new Condition(Operator.LT, Double.toString(randomDouble()));
            if (randomBoolean()) {
                fieldName = randomAsciiOfLengthBetween(1, 20);
                fieldValue = randomAsciiOfLengthBetween(1, 20);
            }
            break;
        }
        return new RuleCondition(r, fieldName, fieldValue, condition, valueList);
    }

    @Override
    protected Reader<RuleCondition> instanceReader() {
        return RuleCondition::new;
    }

    @Override
    protected RuleCondition parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return RuleCondition.PARSER.apply(parser, () -> matcher);
    }

    public void testConstructor() {
        RuleCondition condition = new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "valueList");
        assertEquals(RuleConditionType.CATEGORICAL, condition.getConditionType());
        assertNull(condition.getFieldName());
        assertNull(condition.getFieldValue());
        assertNull(condition.getCondition());
    }

    public void testEqualsGivenSameObject() {
        RuleCondition condition = new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "valueList");
        assertTrue(condition.equals(condition));
    }

    public void testEqualsGivenString() {
        assertFalse(new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "list").equals("a string"));
    }

    public void testEqualsGivenDifferentType() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "valueList");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    public void testEqualsGivenDifferentFieldName() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metricNameaaa", "cpu",
                new Condition(Operator.LT, "5"), null);
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    public void testEqualsGivenDifferentFieldValue() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metricName", "cpuaaa",
                new Condition(Operator.LT, "5"), null);
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    public void testEqualsGivenDifferentCondition() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metricName", "cpu",
                new Condition(Operator.GT, "5"), null);
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    public void testEqualsGivenDifferentValueList() {
        RuleCondition condition1 = new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "myList");
        RuleCondition condition2 = new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "myListaaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    private static RuleCondition createFullyPopulated() {
        return new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metricName", "cpu", new Condition(Operator.LT, "5"), null);
    }

    public void testVerify_GivenCategoricalWithCondition() {
        Condition condition = new Condition(Operator.MATCH, "text");
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.CATEGORICAL, null, null, condition, null));
        assertEquals("Invalid detector rule: a categorical ruleCondition does not support condition", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenCategoricalWithFieldValue() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.CATEGORICAL, "metric", "CPU", null, null));
        assertEquals("Invalid detector rule: a categorical ruleCondition does not support fieldValue", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenCategoricalWithoutValueList() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, null));
        assertEquals("Invalid detector rule: a categorical ruleCondition requires valueList to be set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalActualWithValueList() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, null, "myList"));
        assertEquals("Invalid detector rule: a numerical ruleCondition does not support valueList", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalActualWithoutCondition() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, null, null));
        assertEquals("Invalid detector rule: a numerical ruleCondition requires condition to be set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalActualWithFieldNameButNoFieldValue() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metric", null, new Condition(Operator.LT, "5"), null));
        assertEquals("Invalid detector rule: a numerical ruleCondition with fieldName requires that fieldValue is set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalTypicalWithValueList() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, null, "myList"));
        assertEquals("Invalid detector rule: a numerical ruleCondition does not support valueList", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalTypicalWithoutCondition() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, null, null));
        assertEquals("Invalid detector rule: a numerical ruleCondition requires condition to be set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalDiffAbsWithValueList() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_DIFF_ABS, null, null, null, "myList"));
        assertEquals("Invalid detector rule: a numerical ruleCondition does not support valueList", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalDiffAbsWithoutCondition() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_DIFF_ABS, null, null, null, null));
        assertEquals("Invalid detector rule: a numerical ruleCondition requires condition to be set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenFieldValueWithoutFieldName() {
        Condition condition = new Condition(Operator.LTE, "5");
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_DIFF_ABS, null, "foo", condition, null));
        assertEquals("Invalid detector rule: missing fieldName in ruleCondition where fieldValue 'foo' is set", e.getMessage());
        assertEquals(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalAndOperatorEquals() {
        Condition condition = new Condition(Operator.EQ, "5");
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, condition, null));
        assertEquals("Invalid detector rule: operator 'EQ' is not allowed", e.getMessage());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenNumericalAndOperatorMatch() {
        Condition condition = new Condition(Operator.MATCH, "aaa");
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, condition, null));
        assertEquals("Invalid detector rule: operator 'MATCH' is not allowed", e.getMessage());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenDetectionRuleWithInvalidCondition() {
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metricName", "CPU", new Condition(Operator.LT, "invalid"),
                        null));
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, "invalid"), e.getMessage());
    }

    public void testVerify_GivenValidCategorical() {
        // no validation error:
        new RuleCondition(RuleConditionType.CATEGORICAL, "metric", null, null, "myList");
    }

    public void testVerify_GivenValidNumericalActual() {
        // no validation error:
        new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metric", "cpu", new Condition(Operator.GT, "5"), null);
    }

    public void testVerify_GivenValidNumericalTypical() {
        // no validation error:
        new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "metric", "cpu", new Condition(Operator.GTE, "5"), null);
    }

    public void testVerify_GivenValidNumericalDiffAbs() {
        // no validation error:
        new RuleCondition(RuleConditionType.NUMERICAL_DIFF_ABS, "metric", "cpu", new Condition(Operator.LT, "5"), null);
    }

}
