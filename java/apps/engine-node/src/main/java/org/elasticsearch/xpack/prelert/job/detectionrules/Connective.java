
package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

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
        return Connective.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
