
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.util.Locale;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

public enum RuleConditionType implements Writeable {
    CATEGORICAL("categorical"), 
    NUMERICAL_ACTUAL("numerical_actual"), 
    NUMERICAL_TYPICAL("numerical_typical"), 
    NUMERICAL_DIFF_ABS("numerical_diff_abs");
    
    private String name;

    private RuleConditionType(String name) {
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
     * @return The condition type
     */
    @JsonCreator
    public static RuleConditionType forString(String value) {
        return RuleConditionType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static RuleConditionType readFromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown RuleConditionType ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
