
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class DetectionRuleTest extends ESTestCase {

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
        RuleCondition numericalCondition = new RuleCondition();
        numericalCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        rule.setRuleConditions(Arrays.asList(
                numericalCondition,
                RuleCondition.createCategorical("foo", "list1"),
                RuleCondition.createCategorical("bar", "list2")));

        assertEquals(new HashSet<String>(Arrays.asList("list1", "list2")), rule.extractReferencedLists());
    }


    public void testEqualsGivenSameObject() {
        DetectionRule rule = new DetectionRule();
        assertTrue(rule.equals(rule));
    }


    public void testEqualsGivenString() {
        assertFalse(new DetectionRule().equals("a string"));
    }


    public void testEqualsGivenDifferentAction() {
        DetectionRule rule1 = createFullyPopulated();
        DetectionRule rule2 = createFullyPopulated();
        rule2.setRuleAction(null);
        assertFalse(rule1.equals(rule2));
        assertFalse(rule2.equals(rule1));
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
        rule.setRuleAction(RuleAction.FILTER_RESULTS);
        rule.setTargetFieldName("targetField");
        rule.setTargetFieldValue("targetValue");
        rule.setConditionsConnective(Connective.AND);
        rule.setRuleConditions(Arrays.asList(new RuleCondition()));
        return rule;
    }
}
