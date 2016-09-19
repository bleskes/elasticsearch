
package org.elasticsearch.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Connective
{
    OR, AND;

    /**
     * Case-insensitive from string method.
     *
     * @param value String representation
     * @return The connective type
     */
    @JsonCreator
    public static Connective forString(String value)
    {
        return Connective.valueOf(value.toUpperCase());
    }
}
