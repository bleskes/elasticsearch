/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.data;

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

import java.io.InputStream;
import java.util.function.Supplier;

public interface DataProcessor {

    /**
     * Passes data to the native process.
     * This is a blocking call that won't return until all the data has been
     * written to the process.
     *
     * An ElasticsearchStatusException will be thrown is any of these error conditions occur:
     * <ol>
     *     <li>If a configured field is missing from the CSV header</li>
     *     <li>If JSON data is malformed and we cannot recover parsing</li>
     *     <li>If a high proportion of the records the timestamp field that cannot be parsed</li>
     *     <li>If a high proportion of the records chronologically out of order</li>
     * </ol>
     *
     * @param jobId     the jobId
     * @param input     Data input stream
     * @param params    Data processing parameters
     * @param cancelled Whether the data processing has been cancelled
     * @return Count of records, fields, bytes, etc written
     */
    DataCounts processData(String jobId, InputStream input, DataLoadParams params, Supplier<Boolean> cancelled);

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param interimResultsParams Parameters about whether interim results calculation
     * should occur and for which period of time
     */
    void flushJob(String jobId, InterimResultsParams interimResultsParams);

    void openJob(String jobId, boolean ignoreDowntime);

    /**
     * Stop the running job and mark it as finished.<br>
     *  @param jobId The job to stop
     *
     */
    void closeJob(String jobId);
}
