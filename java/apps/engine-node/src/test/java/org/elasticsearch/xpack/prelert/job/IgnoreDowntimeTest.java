
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.test.ESTestCase;

public class IgnoreDowntimeTest extends ESTestCase {

    public void testFromString_GivenLeadingWhitespace() {
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString(" \t ALWAYS"));
    }


    public void testFromString_GivenTrailingWhitespace() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER \t "));
    }


    public void testFromString_GivenExactMatches() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("ONCE"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("ALWAYS"));
    }


    public void testFromString_GivenMixedCaseCharacters() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("nevEr"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("oNce"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("always"));
    }

    public void testFromString_GivenNonMatchingString() {
        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> IgnoreDowntime.fromString("nope"));
    }
}
