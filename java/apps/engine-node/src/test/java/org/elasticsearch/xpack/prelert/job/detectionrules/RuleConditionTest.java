
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.junit.Test;

import static org.junit.Assert.*;

public class RuleConditionTest {
    @Test
    public void testDefaultConstructor() {
        RuleCondition condition = new RuleCondition();
        assertNull(condition.getConditionType());
        assertNull(condition.getFieldName());
        assertNull(condition.getFieldValue());
        assertNull(condition.getCondition());
    }

    @Test
    public void testEqualsGivenSameObject() {
        RuleCondition condition = new RuleCondition();
        assertTrue(condition.equals(condition));
    }

    @Test
    public void testEqualsGivenString() {
        assertFalse(new RuleCondition().equals("a string"));
    }

    @Test
    public void testEqualsGivenDifferentType() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setConditionType(RuleConditionType.CATEGORICAL);
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentFieldName() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldName(condition2.getFieldName() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentFieldValue() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldValue(condition2.getFieldValue() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentCondition() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setCondition(new Condition(Operator.GT, "5"));
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenDifferentValueList() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setValueList(condition2.getValueList() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }

    @Test
    public void testEqualsGivenEqual() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        assertTrue(condition1.equals(condition2));
        assertTrue(condition2.equals(condition1));
        assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    private static RuleCondition createFullyPopulated() {
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setFieldName("metricName");
        condition.setFieldValue("cpu");
        condition.setCondition(new Condition(Operator.LT, "5"));
        condition.setValueList("myList");
        return condition;
    }
}
