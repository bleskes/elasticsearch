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
package org.elasticsearch.xpack.prelert.job.data;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

public class DataStreamer {
    private static final Logger LOGGER = Loggers.getLogger(DataStreamer.class);

    private final DataProcessor dataProccesor;

    public DataStreamer(DataProcessor dataProcessor) {
        dataProccesor = Objects.requireNonNull(dataProcessor);
    }

    /**
     * Stream the data to the native process.
     *
     * @return Count of records, fields, bytes, etc written
     */
    public DataCounts streamData(String contentEncoding, String jobId, InputStream input, DataLoadParams params) throws IOException {
        LOGGER.trace("Handle Post data to job {} ", jobId);

        input = tryDecompressingInputStream(contentEncoding, jobId, input);
        DataCounts stats = handleStream(jobId, input, params);

        LOGGER.debug("Data uploaded to job {}", jobId);

        return stats;
    }

    private InputStream tryDecompressingInputStream(String contentEncoding, String jobId, InputStream input) throws IOException {
        if ("gzip".equals(contentEncoding)) {
            LOGGER.debug("Decompressing post data in job {}", jobId);
            try {
                return new GZIPInputStream(input);
            } catch (ZipException ze) {
                LOGGER.error("Failed to decompress data file", ze);
                throw new IllegalArgumentException(Messages.getMessage(Messages.REST_GZIP_ERROR), ze);
            }
        }
        return input;
    }

    /**
     * Pass the data stream to the native process.
     *
     * @return Count of records, fields, bytes, etc written
     */
    private DataCounts handleStream(String jobId, InputStream input, DataLoadParams params) {
        return dataProccesor.processData(jobId, input, params);
    }
}
