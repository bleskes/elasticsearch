
package org.elasticsearch.xpack.prelert.job.status;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Records sent to autodetect should be in ascending chronological
 * order else they are ignored and a error logged. This exception
 * represents the case where a high proportion of messages are not
 * in temporal order.
 */
public class OutOfOrderRecordsException extends JobException {
    private static final long serialVersionUID = -7088347813900268191L;

    private final long numberBad;
    private final long totalNumber;

    public OutOfOrderRecordsException(long numberBadRecords, long totalNumberRecords) {
        super(String.format("A high proportion of records are not in ascending chronological order (%d of %d) and/or not within latency.",
                numberBadRecords, totalNumberRecords), ErrorCodes.TOO_MANY_OUT_OF_ORDER_RECORDS);

        numberBad = numberBadRecords;
        totalNumber = totalNumberRecords;
    }

    /**
     * The number of out of order records
     *
     * @return
     */
    public long getNumberOutOfOrder() {
        return numberBad;
    }

    /**
     * Total number of records (good + bad)
     *
     * @return
     */
    public long getTotalNumber() {
        return totalNumber;
    }
}
