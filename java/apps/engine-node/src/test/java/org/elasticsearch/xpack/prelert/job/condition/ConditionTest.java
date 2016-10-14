
package org.elasticsearch.xpack.prelert.job.condition;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class ConditionTest extends AbstractSerializingTestCase<Condition> {

    public void testSetValues() {
        // When the args can't be parsed the
        // default is the < operator and 0.
        Condition cond = Condition.NONE;
        assertEquals(Operator.NONE, cond.getOperator());
        assertEquals(null, cond.getValue());

        cond = new Condition(Operator.EQ, "astring");
        assertEquals(Operator.EQ, cond.getOperator());
        assertEquals("astring", cond.getValue());
    }


    public void testHashCodeAndEquals() {
        Condition cond1 = new Condition(Operator.MATCH, "regex");
        Condition cond2 = new Condition(Operator.MATCH, "regex");

        assertEquals(cond1, cond2);
        assertEquals(cond1.hashCode(), cond2.hashCode());

        Condition cond3 = new Condition(Operator.EQ, "regex");
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
            condition = new Condition(op, randomAsciiOfLengthBetween(1, 20));
            break;
        case MATCH:
            condition = new Condition(op, randomAsciiOfLengthBetween(1, 20));
            break;
        case NONE:
            condition = new Condition(op, null);
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
    protected Condition parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Condition.PARSER.apply(parser, () -> matcher);
    }
}
