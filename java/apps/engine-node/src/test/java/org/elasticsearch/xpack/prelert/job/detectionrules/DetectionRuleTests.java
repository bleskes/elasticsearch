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

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class DetectionRuleTests extends AbstractSerializingTestCase<DetectionRule> {

    public void testDefaultConstructor() {
        DetectionRule rule = new DetectionRule();
        assertEquals(RuleAction.FILTER_RESULTS, rule.getRuleAction());
        assertEquals(Connective.OR, rule.getConditionsConnective());
        assertEquals(Collections.emptyList(), rule.getRuleConditions());
        assertNull(rule.getTargetFieldName());
        assertNull(rule.getTargetFieldValue());
    }


    public void testExtractReferencedLists() {
        DetectionRule rule = new DetectionRule();
        RuleCondition numericalCondition =
                new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "field", "value", new Condition(Operator.GT, "5"), null);
        rule.setRuleConditions(Arrays.asList(
                numericalCondition,
                RuleCondition.createCategorical("foo", "list1"),
                RuleCondition.createCategorical("bar", "list2")));

        assertEquals(new HashSet<>(Arrays.asList("list1", "list2")), rule.extractReferencedLists());
    }


    public void testEqualsGivenSameObject() {
        DetectionRule rule = new DetectionRule();
        assertTrue(rule.equals(rule));
    }


    public void testEqualsGivenString() {
        assertFalse(new DetectionRule().equals("a string"));
    }


    public void testEqualsGivenDifferentTargetFieldName() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldName(rule2.getTargetFieldName() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }


    public void testEqualsGivenDifferentTargetFieldValue() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldValue(rule2.getTargetFieldValue() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }


    public void testEqualsGivenDifferentConjunction() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setConditionsConnective(Connective.OR);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }


    public void testEqualsGivenRules() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleConditions(Collections.emptyList());
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }


    public void testEqualsGivenEqual() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        assertTrue(rule1.equals(rule2));
        assertTrue(rule2.equals(rule1));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    private static DetectionRule createFullyPopulated() {
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("targetField");
        rule.setTargetFieldValue("targetValue");
        rule.setConditionsConnective(Connective.AND);
        rule.setRuleConditions(Arrays.asList(new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, "myList")));
        return rule;
    }

    @Override
    protected DetectionRule createTestInstance() {
        DetectionRule detectionRule = new DetectionRule();
        if (randomBoolean()) {
            detectionRule.setConditionsConnective(randomFrom(Connective.values()));
        }
        if (randomBoolean()) {
            int size = randomInt(20);
            List<RuleCondition> ruleConditions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                // no need for random condition (it is already tested)
                RuleCondition condition =
                        new RuleCondition(RuleConditionType.CATEGORICAL, null, null, null, randomAsciiOfLengthBetween(1, 20));
                ruleConditions.add(condition);
            }
            detectionRule.setRuleConditions(ruleConditions);
        }
        if (randomBoolean()) {
            detectionRule.setTargetFieldName(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            detectionRule.setTargetFieldValue(randomAsciiOfLengthBetween(1, 20));
        }
        return detectionRule;
    }

    @Override
    protected Reader<DetectionRule> instanceReader() {
        return DetectionRule::new;
    }

    @Override
    protected DetectionRule parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return DetectionRule.PARSER.apply(parser, () -> matcher);
    }
}
