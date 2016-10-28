
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import java.io.IOException;
import java.util.List;

/**
 * Interface for classes that write arrays of strings to the
 * Prelert analytics processes.
 */
public interface RecordWriter {
    /**
     * Value must match api::CAnomalyDetector::CONTROL_FIELD_NAME in the C++
     * code.
     */
    public static final String CONTROL_FIELD_NAME = ".";

    /**
     * Write each String in the record array
     */
    public abstract void writeRecord(String[] record) throws IOException;

    /**
     * Write each String in the record list
     */
    public abstract void writeRecord(List<String> record) throws IOException;

    /**
     * Flush the outputIndex stream.
     */
    public abstract void flush() throws IOException;

}