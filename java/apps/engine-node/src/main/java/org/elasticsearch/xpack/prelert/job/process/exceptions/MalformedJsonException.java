
package org.elasticsearch.xpack.prelert.job.process.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

public class MalformedJsonException extends JobException {

    private static final long serialVersionUID = 3484291061126337921L;

    public MalformedJsonException(Throwable cause) {
        super("The input JSON data is malformed.", ErrorCodes.MALFORMED_JSON, cause);
    }
}
