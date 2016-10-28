
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.DataCounts;

/**
 * Update a job's dataCounts
 * i.e. the number of processed records, fields etc.
 */
public interface JobDataCountsPersister
{
    /**
     * Update the job's data counts stats and figures.
     *
     * @param jobId Job to update
     * @param counts The counts
     */
    void persistDataCounts(String jobId, DataCounts counts);
}
