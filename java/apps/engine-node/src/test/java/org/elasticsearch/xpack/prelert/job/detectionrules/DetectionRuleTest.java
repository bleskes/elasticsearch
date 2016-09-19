
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class DetectionRuleTest {
    @Test
    public void testDefaultConstructor() {
        DetectionRule rule = new DetectionRule();
        assertEquals(RuleAction.FILTER_RESULTS, rule.getRuleAction());
        assertEquals(Connective.OR, rule.getConditionsConnective());
        assertEquals(Collections.emptyList(), rule.getRuleConditions());
        assertNull(rule.getTargetFieldName());
        assertNull(rule.getTargetFieldValue());
    }

    @Test
    public void testExtractReferencedLists() {
        DetectionRule rule = new DetectionRule();
        RuleCondition numericalCondition = new RuleCondition();
        numericalCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        rule.setRuleConditions(Arrays.asList(
                numericalCondition,
                RuleCondition.createCategorical("foo", "list1"),
                RuleCondition.createCategorical("bar", "list2")));

        assertEquals(Sets.newHashSet("list1", "list2"), rule.extractReferencedLists());
    }

    @Test
    public void testEqualsGivenSameObject() {
        DetectionRule rule = new DetectionRule();
        assertTrue(rule.equals(rule));
    }

    @Test
    public void testEqualsGivenString() {
        assertFalse(new DetectionRule().equals("a string"));
    }

    @Test
    public void testEqualsGivenDifferentAction() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleAction(null);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentTargetFieldName() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldName(rule2.getTargetFieldName() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentTargetFieldValue() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setTargetFieldValue(rule2.getTargetFieldValue() + "2");
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenDifferentConjunction() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setConditionsConnective(Connective.OR);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenRules() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleConditions(Collections.emptyList());
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
    }

    @Test
    public void testEqualsGivenEqual() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        assertTrue(rule1.equals(rule2));
        assertTrue(rule2.equals(rule1));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    private static DetectionRule createFullyPopulated() {
        DetectionRule rule = new DetectionRule();
        rule.setRuleAction(RuleAction.FILTER_RESULTS);
        rule.setTargetFieldName("targetField");
        rule.setTargetFieldValue("targetValue");
        rule.setConditionsConnective(Connective.AND);
        rule.setRuleConditions(Arrays.asList(new RuleCondition()));
        return rule;
    }
}
