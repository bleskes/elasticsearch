
package org.elasticsearch.xpack.prelert.job.usage;

/**
 * Defines the field names/mappings for the stored
 * usage documents.
 */
public final class Usage {
    public static final String TYPE = "usage";
    public static final String TIMESTAMP = "timestamp";
    public static final String INPUT_BYTES = "inputBytes";
    public static final String INPUT_FIELD_COUNT = "inputFieldCount";
    public static final String INPUT_RECORD_COUNT = "inputRecordCount";

    private Usage() {
    }
}
