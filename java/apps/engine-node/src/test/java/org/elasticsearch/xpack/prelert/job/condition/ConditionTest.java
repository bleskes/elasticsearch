
package org.elasticsearch.xpack.prelert.job.condition;

import org.elasticsearch.test.ESTestCase;

public class ConditionTest extends ESTestCase {

    public void testSetValues() {
        // When the args can't be parsed the
        // default is the < operator and 0.
        Condition cond = new Condition();
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

        cond2.setOperator(Operator.EQ);
        assertFalse(cond1.equals(cond2));
        assertFalse(cond1.hashCode() == cond2.hashCode());
    }
}
