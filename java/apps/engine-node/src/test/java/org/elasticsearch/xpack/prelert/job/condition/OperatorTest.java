
package org.elasticsearch.xpack.prelert.job.condition;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class OperatorTest {

    @Test
    public void testFromString() throws UnknownOperatorException {
        assertEquals(Operator.fromString("gt"), Operator.GT);
        assertEquals(Operator.fromString("Gt"), Operator.GT);
        assertEquals(Operator.fromString("EQ"), Operator.EQ);
        assertEquals(Operator.fromString("eq"), Operator.EQ);
        assertEquals(Operator.fromString("lte"), Operator.LTE);
        assertEquals(Operator.fromString("lt"), Operator.LT);
        assertEquals(Operator.fromString("GTE"), Operator.GTE);
        assertEquals(Operator.fromString("Match"), Operator.MATCH);
    }

    @Test
    public void testTest() {
        assertTrue(Operator.GT.expectsANumericArgument());
        assertTrue(Operator.GT.test(1.0, 0.0));
        assertFalse(Operator.GT.test(0.0, 1.0));

        assertTrue(Operator.GTE.expectsANumericArgument());
        assertTrue(Operator.GTE.test(1.0, 0.0));
        assertTrue(Operator.GTE.test(1.0, 1.0));
        assertFalse(Operator.GTE.test(0.0, 1.0));

        assertTrue(Operator.EQ.expectsANumericArgument());
        assertTrue(Operator.EQ.test(0.0, 0.0));
        assertFalse(Operator.EQ.test(1.0, 0.0));

        assertTrue(Operator.LT.expectsANumericArgument());
        assertTrue(Operator.LT.test(0.0, 1.0));
        assertFalse(Operator.LT.test(0.0, 0.0));

        assertTrue(Operator.LTE.expectsANumericArgument());
        assertTrue(Operator.LTE.test(0.0, 1.0));
        assertTrue(Operator.LTE.test(1.0, 1.0));
        assertFalse(Operator.LTE.test(1.0, 0.0));
    }

    @Test
    public void testMatch() {
        assertFalse(Operator.MATCH.expectsANumericArgument());
        assertFalse(Operator.MATCH.test(0.0, 1.0));

        Pattern pattern = Pattern.compile("^aa.*");

        assertTrue(Operator.MATCH.match(pattern, "aaaaa"));
        assertFalse(Operator.MATCH.match(pattern, "bbaaa"));
    }
}