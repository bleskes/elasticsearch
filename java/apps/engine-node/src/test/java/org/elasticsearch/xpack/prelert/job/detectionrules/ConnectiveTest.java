
package org.elasticsearch.xpack.prelert.job.detectionrules;

import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectiveTest extends ESTestCase {

    public void testForString() {
        assertEquals(Connective.OR, Connective.forString("or"));
        assertEquals(Connective.OR, Connective.forString("OR"));
        assertEquals(Connective.AND, Connective.forString("and"));
        assertEquals(Connective.AND, Connective.forString("AND"));
    }
}
