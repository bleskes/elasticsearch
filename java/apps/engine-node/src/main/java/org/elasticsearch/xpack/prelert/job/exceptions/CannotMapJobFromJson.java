
package org.elasticsearch.xpack.prelert.job.exceptions;

public class CannotMapJobFromJson extends RuntimeException
{

    private static final long serialVersionUID = -4757809349812298392L;

    public CannotMapJobFromJson(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
