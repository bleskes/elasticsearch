
package org.elasticsearch.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

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
        return RuleAction.valueOf(value.toUpperCase());
    }
}
