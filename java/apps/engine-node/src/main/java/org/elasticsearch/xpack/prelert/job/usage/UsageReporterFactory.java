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
package org.elasticsearch.xpack.prelert.job.usage;

import org.apache.logging.log4j.Logger;

/**
 * Abstract Factory method for creating new {@link UsageReporter}
 * instances.
 */
public interface UsageReporterFactory {
    /**
     * Return a new UsageReporter for the given job id.
     */
    UsageReporter newUsageReporter(String jobId, Logger logger);
}


