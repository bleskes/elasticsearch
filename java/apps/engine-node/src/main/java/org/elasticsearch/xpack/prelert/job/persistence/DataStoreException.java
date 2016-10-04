
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

public class DataStoreException extends JobException
{
    private static final long serialVersionUID = 3297520527560841022L;

    public DataStoreException(String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
    }

    public DataStoreException(String message, ErrorCodes errorCode, Throwable cause)
    {
        super(message, errorCode, cause);
    }
}
