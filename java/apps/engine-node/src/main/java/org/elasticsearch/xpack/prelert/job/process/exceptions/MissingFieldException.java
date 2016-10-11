
package org.elasticsearch.xpack.prelert.job.process.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Represents the case where a job has been configured to use
 * a specific field but that field is missing from the data.
 */
public class MissingFieldException extends JobException {
    private static final long serialVersionUID = -5303432170987377451L;

    private final String missingFieldName;

    public MissingFieldException(String fieldName, String message) {
        super(message, ErrorCodes.MISSING_FIELD);
        missingFieldName = fieldName;
    }

    public String getMissingFieldName() {
        return missingFieldName;
    }
}
