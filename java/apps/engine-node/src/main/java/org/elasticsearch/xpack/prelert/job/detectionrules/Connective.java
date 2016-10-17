
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.util.Locale;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

public enum Connective implements Writeable {
    OR("or"),
    AND("and");

    private String name;

    private Connective(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Case-insensitive from string method.
     *
     * @param value
     *            String representation
     * @return The connective type
     */
    @JsonCreator
    public static Connective fromString(String value) {
        return Connective.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static Connective readFromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown Connective ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
