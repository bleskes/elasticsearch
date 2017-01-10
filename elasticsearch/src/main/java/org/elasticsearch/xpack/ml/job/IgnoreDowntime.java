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

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.Locale;

public enum IgnoreDowntime implements Writeable {

    NEVER, ONCE, ALWAYS;

    /**
     * <p>
     * Parses a string and returns the corresponding enum value.
     * </p>
     * <p>
     * The method differs from {@link #valueOf(String)} by being
     * able to handle leading/trailing whitespace and being case
     * insensitive.
     * </p>
     * <p>
     * If there is no match {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param value A String that should match one of the enum values
     * @return the matching enum value
     */
    public static IgnoreDowntime fromString(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static IgnoreDowntime fromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown public enum IgnoreDowntime {\n ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
