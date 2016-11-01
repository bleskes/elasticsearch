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
package org.elasticsearch.xpack.prelert.job.status;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

import java.util.Locale;

/**
 * Records sent to autodetect should be in ascending chronological
 * order else they are ignored and a error logged. This exception
 * represents the case where a high proportion of messages are not
 * in temporal order.
 */
public class OutOfOrderRecordsException extends JobException {

    private final long numberBad;
    private final long totalNumber;

    public OutOfOrderRecordsException(long numberBadRecords, long totalNumberRecords) {
        super(String.format(Locale.ROOT,
                "A high proportion of records are not in ascending chronological order (%d of %d) and/or not within latency.",
                numberBadRecords, totalNumberRecords), ErrorCodes.TOO_MANY_OUT_OF_ORDER_RECORDS);

        numberBad = numberBadRecords;
        totalNumber = totalNumberRecords;
    }

    /**
     * The number of out of order records
     */
    public long getNumberOutOfOrder() {
        return numberBad;
    }

    /**
     * Total number of records (good + bad)
     */
    public long getTotalNumber() {
        return totalNumber;
    }
}
