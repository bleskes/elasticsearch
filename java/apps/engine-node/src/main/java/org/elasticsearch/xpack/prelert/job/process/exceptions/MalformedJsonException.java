
package org.elasticsearch.xpack.prelert.job.process.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

public class MalformedJsonException extends JobException {


    public MalformedJsonException(Throwable cause) {
        super("The input JSON data is malformed.", ErrorCodes.MALFORMED_JSON, cause);
    }
}
