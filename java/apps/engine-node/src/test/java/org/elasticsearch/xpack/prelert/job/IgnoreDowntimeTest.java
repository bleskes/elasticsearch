
package org.elasticsearch.xpack.prelert.job;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IgnoreDowntimeTest {
    @Test
    public void testFromString_GivenLeadingWhitespace() {
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString(" \t ALWAYS"));
    }

    @Test
    public void testFromString_GivenTrailingWhitespace() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER \t "));
    }

    @Test
    public void testFromString_GivenExactMatches() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("NEVER"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("ONCE"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("ALWAYS"));
    }

    @Test
    public void testFromString_GivenMixedCaseCharacters() {
        assertEquals(IgnoreDowntime.NEVER, IgnoreDowntime.fromString("nevEr"));
        assertEquals(IgnoreDowntime.ONCE, IgnoreDowntime.fromString("oNce"));
        assertEquals(IgnoreDowntime.ALWAYS, IgnoreDowntime.fromString("always"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_GivenNonMatchingString() {
        IgnoreDowntime.fromString("nope");
    }
}
