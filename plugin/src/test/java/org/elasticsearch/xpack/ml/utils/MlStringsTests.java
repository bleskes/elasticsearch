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
package org.elasticsearch.xpack.ml.utils;


import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class MlStringsTests extends ESTestCase {
    public void testDoubleQuoteIfNotAlphaNumeric() {
        assertEquals("foo2", MlStrings.doubleQuoteIfNotAlphaNumeric("foo2"));
        assertEquals("\"fo o\"", MlStrings.doubleQuoteIfNotAlphaNumeric("fo o"));
        assertEquals("\" \"", MlStrings.doubleQuoteIfNotAlphaNumeric(" "));
        assertEquals("\"ba\\\"r\\\"\"", MlStrings.doubleQuoteIfNotAlphaNumeric("ba\"r\""));
    }

    public void testIsValidId() {
        assertThat(MlStrings.isValidId("1_-.a"), is(true));
        assertThat(MlStrings.isValidId("b.-_3"), is(true));
        assertThat(MlStrings.isValidId("a-b.c_d"), is(true));

        assertThat(MlStrings.isValidId("a1_-."), is(false));
        assertThat(MlStrings.isValidId("-.a1_"), is(false));
        assertThat(MlStrings.isValidId(".a1_-"), is(false));
        assertThat(MlStrings.isValidId("_-.a1"), is(false));
        assertThat(MlStrings.isValidId("A"), is(false));
        assertThat(MlStrings.isValidId("!afafd"), is(false));
        assertThat(MlStrings.isValidId("_all"), is(false));
    }

    public void testGetParentField() {
        assertThat(MlStrings.getParentField(null), is(nullValue()));
        assertThat(MlStrings.getParentField("foo"), equalTo("foo"));
        assertThat(MlStrings.getParentField("foo.bar"), equalTo("foo"));
        assertThat(MlStrings.getParentField("x.y.z"), equalTo("x.y"));
    }
}
