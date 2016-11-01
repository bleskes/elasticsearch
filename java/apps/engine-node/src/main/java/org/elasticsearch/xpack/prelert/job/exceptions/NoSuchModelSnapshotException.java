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
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * This type of exception represents an error where
 * an operation uses a <i>SnapshotId</i> that does not exist.
 */
public class NoSuchModelSnapshotException extends JobException
{

    public NoSuchModelSnapshotException(String jobId)
    {
        super(Messages.getMessage(Messages.REST_NO_SUCH_MODEL_SNAPSHOT, jobId),
                ErrorCodes.NO_SUCH_MODEL_SNAPSHOT);
    }
}
