package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

/**
 * Interface representing the native C++ autodetect process
 */
public interface AutodetectProcess {

    /**
     * Write the record to autodetect. The record parameter should not be encoded
     * (i.e. length encoded) the implementation will appy the corrrect encoding.
     *
     * @param record Plain array of strings, implementors of this class should
     *               encode the record appropriately
     * @throws IOException If the write failed
     */
    void writeRecord(String [] record) throws IOException;

    /**
     * Write the reset buckets control message
     * @param params Reset bucket options
     * @throws IOException If write reset mesage fails
     */
    void writeResetBucketsControlMessage(DataLoadParams params) throws IOException;

    /**
     * Write an update configuration message
     * @param config Config message
     * @throws IOException If the write config message fails
     */
    void writeUpdateConfigMessage(String config) throws IOException;

    /**
     * Flush the job pushing any stale data into autodetect
     * @param params Should interim results be generated
     * @throws IOException If the flush failed
     */
    void flushJob(InterimResultsParams params) throws IOException;

    /**
     * Flush the output data stream
     * @return
     * @throws IOException
     */
    void flushStream() throws IOException;

    /**
     * Close
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Autodetect's error stream
     * @return error inputstream
     */
    InputStream error();

    /**
     * Autodetect's output stream
     * @return output stream
     */
    InputStream out();

    /**
     * The time the process was started
     * @return Process start time
     */
    ZonedDateTime getProcessStartTime();
}
