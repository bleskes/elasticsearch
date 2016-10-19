package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum JobSchedulerStatus implements Writeable {

    STARTED, STOPPING, STOPPED;

    @JsonCreator
    public static JobSchedulerStatus fromString(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public static JobSchedulerStatus fromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown public enum JobSchedulerStatus {\n ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }

}
