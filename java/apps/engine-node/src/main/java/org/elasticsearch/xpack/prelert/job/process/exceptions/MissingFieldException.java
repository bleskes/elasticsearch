/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.process.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Represents the case where a job has been configured to use
 * a specific field but that field is missing from the data.
 */
public class MissingFieldException extends JobException {

    private final String missingFieldName;

    public MissingFieldException(String fieldName, String message) {
        super(message, ErrorCodes.MISSING_FIELD);
        missingFieldName = fieldName;
    }

    public String getMissingFieldName() {
        return missingFieldName;
    }
}
