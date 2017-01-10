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

import java.io.IOException;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.DataDescription.DataFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class DataFormatTests extends ESTestCase {

    public void testDataFormatForString() {
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("delineated"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("DELINEATED"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("delimited"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("DELIMITED"));

        assertEquals(DataFormat.JSON, DataFormat.forString("json"));
        assertEquals(DataFormat.JSON, DataFormat.forString("JSON"));

        assertEquals(DataFormat.SINGLE_LINE, DataFormat.forString("single_line"));
        assertEquals(DataFormat.SINGLE_LINE, DataFormat.forString("SINGLE_LINE"));
    }

    public void testValidOrdinals() {
        assertThat(DataFormat.JSON.ordinal(), equalTo(0));
        assertThat(DataFormat.DELIMITED.ordinal(), equalTo(1));
        assertThat(DataFormat.SINGLE_LINE.ordinal(), equalTo(2));
        assertThat(DataFormat.ELASTICSEARCH.ordinal(), equalTo(3));
    }

    public void testwriteTo() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            DataFormat.JSON.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(0));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            DataFormat.DELIMITED.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(1));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            DataFormat.SINGLE_LINE.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(2));
            }
        }

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            DataFormat.ELASTICSEARCH.writeTo(out);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(in.readVInt(), equalTo(3));
            }
        }
    }

    public void testReadFrom() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(0);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(DataFormat.readFromStream(in), equalTo(DataFormat.JSON));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(1);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(DataFormat.readFromStream(in), equalTo(DataFormat.DELIMITED));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(2);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(DataFormat.readFromStream(in), equalTo(DataFormat.SINGLE_LINE));
            }
        }
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(3);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(DataFormat.readFromStream(in), equalTo(DataFormat.ELASTICSEARCH));
            }
        }
    }

    public void testInvalidReadFrom() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(randomIntBetween(4, Integer.MAX_VALUE));
            try (StreamInput in = out.bytes().streamInput()) {
                DataFormat.readFromStream(in);
                fail("Expected IOException");
            } catch (IOException e) {
                assertThat(e.getMessage(), containsString("Unknown DataFormat ordinal ["));
            }
        }
    }

}
