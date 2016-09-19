
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectiveTest {
    @Test
    public void testForString() {
        assertEquals(Connective.OR, Connective.forString("or"));
        assertEquals(Connective.OR, Connective.forString("OR"));
        assertEquals(Connective.AND, Connective.forString("and"));
        assertEquals(Connective.AND, Connective.forString("AND"));
    }
}
