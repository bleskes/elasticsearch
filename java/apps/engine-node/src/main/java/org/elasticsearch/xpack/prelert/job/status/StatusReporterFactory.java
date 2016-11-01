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
package org.elasticsearch.xpack.prelert.job.status;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;

/**
 * Abstract Factory method for creating new {@link StatusReporter}
 * instances.
 */
public interface StatusReporterFactory {
    /**
     * Return a new StatusReporter for the given job id.
     *
     * @param jobId
     *            the job id
     * @param counts
     *            The persisted counts for the job
     * @param usageReporter
     *            to be analysed in each record. This count does not include the
     *            time field
     * @param logger
     *            The job logger
     */
    StatusReporter newStatusReporter(String jobId, DataCounts
            counts, UsageReporter usageReporter, Logger logger);
}
