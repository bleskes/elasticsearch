
package org.elasticsearch.xpack.prelert.job.status;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * If the timestamp field of a record cannot be read or the
 * date format is incorrect the record is ignored. This
 * exception is thrown when a high proportion of records
 * have a bad timestamp.
 */
public class HighProportionOfBadTimestampsException extends JobException {
    private static final long serialVersionUID = -7776085998658495251L;

    private final long numberBad;
    private final long totalNumber;

    public HighProportionOfBadTimestampsException(long numberBadRecords, long totalNumberRecords) {
        super(String.format("A high proportion of records have a timestamp that cannot be interpreted (%d of %d).",
                numberBadRecords, totalNumberRecords), ErrorCodes.TOO_MANY_BAD_DATES);

        numberBad = numberBadRecords;
        totalNumber = totalNumberRecords;
    }

    /**
     * The number of bad records
     */
    public long getNumberBad() {
        return numberBad;
    }

    /**
     * Total number of records (good + bad)
     */
    public long getTotalNumber() {
        return totalNumber;
    }

}
