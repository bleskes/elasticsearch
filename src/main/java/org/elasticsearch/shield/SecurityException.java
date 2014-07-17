package org.elasticsearch.shield;

import org.elasticsearch.ElasticsearchException;import java.lang.String;import java.lang.Throwable;

/**
 *
 */
public class SecurityException extends ElasticsearchException {


    public SecurityException(String msg) {
        super(msg);
    }

    public SecurityException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
