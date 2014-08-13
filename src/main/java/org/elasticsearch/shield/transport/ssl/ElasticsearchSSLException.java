package org.elasticsearch.shield.transport.ssl;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.rest.RestStatus;

/**
 *
 */
public class ElasticsearchSSLException extends ElasticsearchException {

    public ElasticsearchSSLException(String msg) {
        super(msg);
    }

    public ElasticsearchSSLException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public RestStatus status() {
        return RestStatus.BAD_REQUEST;
    }

}
