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
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.Locale;

public enum SchedulerStatus implements Writeable {

    STARTED, STOPPED;

    public static SchedulerStatus fromString(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public static SchedulerStatus fromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown public enum SchedulerStatus {\n ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
