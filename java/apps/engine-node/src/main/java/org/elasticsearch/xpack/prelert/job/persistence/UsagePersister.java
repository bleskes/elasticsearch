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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Interface for classes that persist usage information
 */
public interface UsagePersister
{
    /**
     * Persist the usage info.
     *
     * @throws JobException IF there was an error persisting
     */
    void persistUsage(String jobId, long bytesRead, long fieldsRead, long recordsRead) throws JobException;
}
