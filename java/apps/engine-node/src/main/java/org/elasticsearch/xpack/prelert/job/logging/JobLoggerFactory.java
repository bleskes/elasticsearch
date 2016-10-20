
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
}
