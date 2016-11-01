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
package org.elasticsearch.xpack.prelert.job.process.exceptions;


import org.elasticsearch.xpack.prelert.job.DataCounts;

import java.util.Locale;

/**
 * This exception is meant to be a wrapper around RuntimeExceptions
 * that may be thrown during data upload. The exception message
 * provides info of the status of the upload up until the point
 * it failed in order to facilitate users to recover.
 */
public class DataUploadException extends RuntimeException {


    private static final String MSG_FORMAT = "An error occurred after processing %d records. "
            + "(invalidDateCount = %d, missingFieldCount = %d, outOfOrderTimeStampCount = %d)";

    public DataUploadException(DataCounts dataCounts, Throwable cause) {
        super(String.format(Locale.ROOT, MSG_FORMAT,
                dataCounts.getInputRecordCount(),
                dataCounts.getInvalidDateCount(),
                dataCounts.getMissingFieldCount(),
                dataCounts.getOutOfOrderTimeStampCount()),
                cause);
    }
}
