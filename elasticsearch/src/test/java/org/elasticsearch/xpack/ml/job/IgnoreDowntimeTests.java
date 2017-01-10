/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job;

import org.elasticsearch.test.ESTestCase;

public class IgnoreDowntimeTests extends ESTestCase {

    public void testForString() {
        assertEquals(IgnoreDowntime.fromString("always"), IgnoreDowntime.ALWAYS);
        assertEquals(IgnoreDowntime.fromString("never"), IgnoreDowntime.NEVER);
        assertEquals(IgnoreDowntime.fromString("once"), IgnoreDowntime.ONCE);
    }

    public void testValidOrdinals() {
        assertEquals(0, IgnoreDowntime.NEVER.ordinal());
        assertEquals(1, IgnoreDowntime.ONCE.ordinal());
        assertEquals(2, IgnoreDowntime.ALWAYS.ordinal());
    }

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
