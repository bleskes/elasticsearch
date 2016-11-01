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
package org.elasticsearch.xpack.prelert.job.logging;

import org.apache.logging.log4j.Logger;

/**
 * Factory to create Job specific logger
 */
public interface JobLoggerFactory {
    /**
     * For per Job logging create a new logger to be used exclusively by the job.
     * @param jobId The Job's ID
     * @return A new logger
     */
    Logger newLogger(String jobId);

    default void close(String jobId, Logger logger) {
    }
}
