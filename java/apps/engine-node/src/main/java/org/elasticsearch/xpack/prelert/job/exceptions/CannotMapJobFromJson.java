
package org.elasticsearch.xpack.prelert.job.exceptions;

public class CannotMapJobFromJson extends RuntimeException
{

    public CannotMapJobFromJson(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
