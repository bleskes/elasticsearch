
package org.elasticsearch.xpack.prelert.job.usage;

import org.apache.logging.log4j.Logger;

/**
 * Abstract Factory method for creating new {@link UsageReporter}
 * instances.
 */
public interface UsageReporterFactory {
    /**
     * Return a new UsageReporter for the given job id.
     *
     * @param jobId
     * @param logger The job logger
     * @return
     */
    public UsageReporter newUsageReporter(String jobId, Logger logger);
}


