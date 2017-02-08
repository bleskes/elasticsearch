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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class RuleConditionTypeTests extends ESTestCase {

    public void testFromString() {
        assertEquals(RuleConditionType.CATEGORICAL, RuleConditionType.fromString("categorical"));
        assertEquals(RuleConditionType.CATEGORICAL, RuleConditionType.fromString("CATEGORICAL"));
        assertEquals(RuleConditionType.NUMERICAL_ACTUAL, RuleConditionType.fromString("numerical_actual"));
        assertEquals(RuleConditionType.NUMERICAL_ACTUAL, RuleConditionType.fromString("NUMERICAL_ACTUAL"));
        assertEquals(RuleConditionType.NUMERICAL_TYPICAL, RuleConditionType.fromString("numerical_typical"));
        assertEquals(RuleConditionType.NUMERICAL_TYPICAL, RuleConditionType.fromString("NUMERICAL_TYPICAL"));
        assertEquals(RuleConditionType.NUMERICAL_DIFF_ABS, RuleConditionType.fromString("numerical_diff_abs"));
        assertEquals(RuleConditionType.NUMERICAL_DIFF_ABS, RuleConditionType.fromString("NUMERICAL_DIFF_ABS"));
    }

    public void testToString() {
        assertEquals("categorical", RuleConditionType.CATEGORICAL.toString());
        assertEquals("numerical_actual", RuleConditionType.NUMERICAL_ACTUAL.toString());
        assertEquals("numerical_typical", RuleConditionType.NUMERICAL_TYPICAL.toString());
        assertEquals("numerical_diff_abs", RuleConditionType.NUMERICAL_DIFF_ABS.toString());
    }

    public void testValidOrdinals() {
        assertThat(RuleConditionType.CATEGORICAL.ordinal(), equalTo(0));
        assertThat(RuleConditionType.NUMERICAL_ACTUAL.ordinal(), equalTo(1));
        assertThat(RuleConditionType.NUMERICAL_TYPICAL.ordinal(), equalTo(2));
        assertThat(RuleConditionType.NUMERICAL_DIFF_ABS.ordinal(), equalTo(3));
    }

    public void testwriteTo() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            RuleConditionType.CATEGORICAL.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(0));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            RuleConditionType.NUMERICAL_ACTUAL.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(1));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            RuleConditionType.NUMERICAL_TYPICAL.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(2));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            RuleConditionType.NUMERICAL_DIFF_ABS.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(3));
            }
        }
    }

    public void testReadFrom() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(0);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(RuleConditionType.readFromStream(in), equalTo(RuleConditionType.CATEGORICAL));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(1);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(RuleConditionType.readFromStream(in), equalTo(RuleConditionType.NUMERICAL_ACTUAL));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(2);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(RuleConditionType.readFromStream(in), equalTo(RuleConditionType.NUMERICAL_TYPICAL));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(3);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(RuleConditionType.readFromStream(in), equalTo(RuleConditionType.NUMERICAL_DIFF_ABS));
            }
        }
    }

    public void testInvalidReadFrom() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(randomIntBetween(4, Integer.MAX_VALUE));
            try (StreamInput in = out.bytes().streamInput()) {
                RuleConditionType.readFromStream(in);
                fail("Expected IOException");
            } catch (IOException e) {
                assertThat(e.getMessage(), containsString("Unknown RuleConditionType ordinal ["));
            }
        }
    }

}
