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
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.elasticsearch.xpack.prelert.job.DataCounts;

/**
 * A writer for transforming and piping data from an
 * inputstream to outputstream as the process expects.
 */
public interface DataToProcessWriter {
    /**
     * Reads the inputIndex, transform to length encoded values and pipe
     * to the OutputStream.
     * If any of the fields in <code>analysisFields</code> or the
     * <code>DataDescription</code>s timeField is missing from the CSV header
     * a <code>MissingFieldException</code> is thrown
     *
     * @return Counts of the records processed, bytes read etc
     */
    DataCounts write(InputStream inputStream, Supplier<Boolean> cancelled) throws IOException;

    /**
     * Flush the outputstream
     */
    void flush() throws IOException;
}
