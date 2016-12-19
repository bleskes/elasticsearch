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
package org.elasticsearch.xpack.prelert.job.process.normalizer;

import org.elasticsearch.xpack.prelert.job.process.normalizer.output.NormalizerResultHandler;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface representing the native C++ normalizer process
 */
public interface NormalizerProcess extends Closeable {

    /**
     * Write the record to normalizer. The record parameter should not be encoded
     * (i.e. length encoded) the implementation will appy the corrrect encoding.
     *
     * @param record Plain array of strings, implementors of this class should
     *               encode the record appropriately
     * @throws IOException If the write failed
     */
    void writeRecord(String[] record) throws IOException;

    /**
     * Create a result handler for this process's results.
     * @return results handler
     */
    NormalizerResultHandler createNormalizedResultsHandler();

    /**
     * Returns true if the process still running.
     * @return True if the process is still running
     */
    boolean isProcessAlive();

    /**
     * Read any content in the error output buffer.
     * @return An error message or empty String if no error.
     */
    String readError();
}
