package org.elasticsearch.xpack.prelert.job.errorcodes;

public interface HasErrorCode {
    String ERROR_CODE_HEADER_KEY = "error_code";

    ErrorCodes getErrorCode();
}
