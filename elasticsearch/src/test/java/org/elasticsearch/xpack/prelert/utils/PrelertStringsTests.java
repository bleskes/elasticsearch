/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.utils;


import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;

public class PrelertStringsTests extends ESTestCase {
    public void testDoubleQuoteIfNotAlphaNumeric() {
        assertEquals("foo2", PrelertStrings.doubleQuoteIfNotAlphaNumeric("foo2"));
        assertEquals("\"fo o\"", PrelertStrings.doubleQuoteIfNotAlphaNumeric("fo o"));
        assertEquals("\" \"", PrelertStrings.doubleQuoteIfNotAlphaNumeric(" "));
        assertEquals("\"ba\\\"r\\\"\"", PrelertStrings.doubleQuoteIfNotAlphaNumeric("ba\"r\""));
    }

    public void testIsValidId() {
        assertThat(PrelertStrings.isValidId("1_-.a"), is(true));
        assertThat(PrelertStrings.isValidId("b.-_3"), is(true));
        assertThat(PrelertStrings.isValidId("a-b.c_d"), is(true));

        assertThat(PrelertStrings.isValidId("a1_-."), is(false));
        assertThat(PrelertStrings.isValidId("-.a1_"), is(false));
        assertThat(PrelertStrings.isValidId(".a1_-"), is(false));
        assertThat(PrelertStrings.isValidId("_-.a1"), is(false));
        assertThat(PrelertStrings.isValidId("A"), is(false));
        assertThat(PrelertStrings.isValidId("!afafd"), is(false));
    }
}
