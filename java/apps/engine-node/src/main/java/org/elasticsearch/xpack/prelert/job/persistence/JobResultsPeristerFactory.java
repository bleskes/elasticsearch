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

/**
 * Create a {@linkplain JobResultsPersister}
 */
public interface JobResultsPeristerFactory {
    /**
     * Get a {@linkplain JobResultsPersister}
     * @param jobId The job to create the persister for.
     * @return The results persister
     */
    JobResultsPersister jobResultsPersister(String jobId);
}
