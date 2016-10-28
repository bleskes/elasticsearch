
package org.elasticsearch.xpack.prelert.job.persistence.serialisation;

import java.util.Map;

/**
 * Interprets field names containing dots as nested JSON structures.
 */
public interface DotNotationReverser
{
    /**
     * Given a field name and value, convert it to a map representation of the
     * (potentially nested) JSON structure.
     */
    void add(String fieldName, String fieldValue);

    /**
     * Return the map representation of the field-value keys that have been added
     */
    Map<String, Object> getResultsMap();

    /**
     * Return the mappings (or schema) of the field-value keys that have been added
     */
    Map<String, Object> getMappingsMap();
}
