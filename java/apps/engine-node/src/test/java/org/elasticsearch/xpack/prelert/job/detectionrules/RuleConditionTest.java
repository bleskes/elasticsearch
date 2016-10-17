
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class RuleConditionTest extends AbstractSerializingTestCase<RuleCondition> {

    public void testConstructor() {
        RuleCondition condition = new RuleCondition(RuleConditionType.CATEGORICAL);
        assertEquals(RuleConditionType.CATEGORICAL, condition.getConditionType());
        assertNull(condition.getFieldName());
        assertNull(condition.getFieldValue());
        assertNull(condition.getCondition());
    }


    public void testEqualsGivenSameObject() {
        RuleCondition condition = new RuleCondition(RuleConditionType.CATEGORICAL);
        assertTrue(condition.equals(condition));
    }


    public void testEqualsGivenString() {
        assertFalse(new RuleCondition(RuleConditionType.CATEGORICAL).equals("a string"));
    }


    public void testEqualsGivenDifferentType() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = new RuleCondition(RuleConditionType.CATEGORICAL);
        condition2.setFieldName("metricName");
        condition2.setFieldValue("cpu");
        condition2.setCondition(new Condition(Operator.LT, "5"));
        condition2.setValueList("myList");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }


    public void testEqualsGivenDifferentFieldName() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldName(condition2.getFieldName() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }


    public void testEqualsGivenDifferentFieldValue() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setFieldValue(condition2.getFieldValue() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }


    public void testEqualsGivenDifferentCondition() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setCondition(new Condition(Operator.GT, "5"));
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }


    public void testEqualsGivenDifferentValueList() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        condition2.setValueList(condition2.getValueList() + "aaa");
        assertFalse(condition1.equals(condition2));
        assertFalse(condition2.equals(condition1));
    }


    public void testEqualsGivenEqual() {
        RuleCondition condition1 = createFullyPopulated();
        RuleCondition condition2 = createFullyPopulated();
        assertTrue(condition1.equals(condition2));
        assertTrue(condition2.equals(condition1));
        assertEquals(condition1.hashCode(), condition2.hashCode());
    }

    private static RuleCondition createFullyPopulated() {
        RuleCondition condition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setFieldName("metricName");
        condition.setFieldValue("cpu");
        condition.setCondition(new Condition(Operator.LT, "5"));
        condition.setValueList("myList");
        return condition;
    }

    @Override
    protected RuleCondition createTestInstance() {
        RuleCondition ruleCondition = new RuleCondition(randomFrom(RuleConditionType.values()));
        if (randomBoolean()) {
            Condition condition = new Condition(randomFrom(Operator.values()), randomAsciiOfLengthBetween(1, 20));
            ruleCondition.setCondition(condition);
        }
        if (randomBoolean()) {
            ruleCondition.setFieldName(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            ruleCondition.setFieldValue(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            ruleCondition.setValueList(randomAsciiOfLengthBetween(1, 20));
        }
        return ruleCondition;
    }

    @Override
    protected Reader<RuleCondition> instanceReader() {
        return RuleCondition::new;
    }

    @Override
    protected RuleCondition parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return RuleCondition.PARSER.apply(parser, () -> matcher);
    }
}
