
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
