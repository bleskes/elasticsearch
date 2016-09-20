
package org.elasticsearch.xpack.prelert.job.exceptions;

public class CannotMapJobFromJson extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CannotMapJobFromJson(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
