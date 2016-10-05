
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum RuleAction
{
    FILTER_RESULTS;

    /**
     * Case-insensitive from string method.
     *
     * @param value String representation
     * @return The rule action
     */
    @JsonCreator
    public static RuleAction forString(String value)
    {
        return RuleAction.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
