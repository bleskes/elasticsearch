package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.elasticsearch.xpack.prelert.job.JobDetails;

/**
 * Factory interface for creating implementations of {@link AutodetectProcess}
 */
public interface AutodetectProcessFactory {
    /**
     *  Create an implementation of {@link AutodetectProcess}
     *
     * @param jobDetails Job configuration for the analysis process
     * @param ignoreDowntime Should gaps in data be treated as anomalous or as a maintenance window after a job re-start
     * @return The process
     */
    AutodetectProcess createAutodetectProcess(JobDetails jobDetails, boolean ignoreDowntime);
}
