package org.elasticsearch.xpack.prelert.job.errorcodes;

public interface HasErrorCode {
    static final String ERROR_CODE_HEADER_KEY = "error_code";

    public ErrorCodes getErrorCode();
}
