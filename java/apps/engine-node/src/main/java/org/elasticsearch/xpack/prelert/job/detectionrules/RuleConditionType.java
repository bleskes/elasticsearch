
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RuleConditionType
{
    CATEGORICAL, NUMERICAL_ACTUAL, NUMERICAL_TYPICAL, NUMERICAL_DIFF_ABS;

    /**
     * Case-insensitive from string method.
     *
     * @param value String representation
     * @return The condition type
     */
    @JsonCreator
    public static RuleConditionType forString(String value)
    {
        return RuleConditionType.valueOf(value.toUpperCase());
    }
}
