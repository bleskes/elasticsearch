/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;

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
     * @throws MissingFieldException                  If any fields are missing from the inputIndex
     * @throws HighProportionOfBadTimestampsException If a large proportion
     *                                                of the records read have missing fields
     * @throws MalformedJsonException                 If JSON data is malformed and we cannot recover.
     */
    DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
    HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
    MalformedJsonException;

    /**
     * Flush the outputstream
     */
    void flush() throws IOException;
}
