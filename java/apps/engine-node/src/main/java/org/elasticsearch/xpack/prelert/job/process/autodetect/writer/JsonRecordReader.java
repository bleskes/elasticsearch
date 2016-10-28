
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import java.io.IOException;

import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;

/**
 * Interface for classes that read the various styles of JSON inputIndex.
 */
interface JsonRecordReader {
    /**
     * Read some JSON and write to the record array.
     *
     * @param record    Read fields are written to this array. This array is first filled with empty
     *                  strings and will never contain a <code>null</code>
     * @param gotFields boolean array each element is true if that field
     *                  was read
     * @return The number of fields in the JSON doc or -1 if nothing was read
     * because the end of the stream was reached
     */
    public long read(String[] record, boolean[] gotFields) throws IOException, MalformedJsonException;
}
