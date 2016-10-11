
package org.elasticsearch.xpack.prelert.job.process.exceptions;


import org.elasticsearch.xpack.prelert.job.DataCounts;

/**
 * This exception is meant to be a wrapper around RuntimeExceptions
 * that may be thrown during data upload. The exception message
 * provides info of the status of the upload up until the point
 * it failed in order to facilitate users to recover.
 */
public class DataUploadException extends RuntimeException {

    private static final long serialVersionUID = -3753422525714929770L;

    private static final String MSG_FORMAT = "An error occurred after processing %d records. "
            + "(invalidDateCount = %d, missingFieldCount = %d, outOfOrderTimeStampCount = %d)";

    public DataUploadException(DataCounts dataCounts, Throwable cause) {
        super(String.format(MSG_FORMAT,
                dataCounts.getInputRecordCount(),
                dataCounts.getInvalidDateCount(),
                dataCounts.getMissingFieldCount(),
                dataCounts.getOutOfOrderTimeStampCount()),
                cause);
    }
}
