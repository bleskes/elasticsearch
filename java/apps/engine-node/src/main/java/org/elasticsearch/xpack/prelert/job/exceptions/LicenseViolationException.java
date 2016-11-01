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
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 * This type of exception represents an error where an operation
 * would result in too many jobs running at the same time.
 */
public class LicenseViolationException extends JobException
{

    /**
     * Create a new LicenseViolationException with an error code
     *
     * @param message Details of error explaining the context
     * @param errorCode the error code
     */
    public LicenseViolationException(String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
    }
}
