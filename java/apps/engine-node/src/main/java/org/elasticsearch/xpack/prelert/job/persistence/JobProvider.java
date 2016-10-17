
package org.elasticsearch.xpack.prelert.job.persistence;


import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.exceptions.NoSuchModelSnapshotException;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

import java.util.Optional;

public interface JobProvider extends JobDetailsProvider, JobResultsProvider
{
    /**
     * Return true if the data store is accessible for the given job Id
     * @param jobId
     * @return
     */
    boolean isConnected(String jobId);

    /**
     * Get the persisted quantiles state for the job
     */
    public Quantiles getQuantiles(String jobId)
    throws UnknownJobException;

    /**
     * Get model snapshots for the job ordered by descending restore priority.
     *
     * @param jobId the job id
     * @param skip number of snapshots to skip
     * @param take number of snapshots to retrieve
     * @return page of model snapshots
     */
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take)
    throws UnknownJobException;

    /**
     * Get model snapshots for the job ordered by descending restore priority.
     *
     * @param jobId the job id
     * @param skip number of snapshots to skip
     * @param take number of snapshots to retrieve
     * @param startEpochMs earliest time to include (inclusive)
     * @param endEpochMs latest time to include (exclusive)
     * @param sortField optional sort field name (may be null)
     * @param sortDescending Sort in descending order
     * @param snapshotId optional snapshot ID to match (null for all)
     * @param description optional description to match (null for all)
     * @return page of model snapshots
     */
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take,
                                                   String startEpochMs, String endEpochMs, String sortField, boolean sortDescending,
                                                   String snapshotId, String description)
    throws UnknownJobException;

    /**
     * Update a persisted model snapshot metadata document to match the
     * argument supplied.
     *
     * @param jobId the job id
     * @param modelSnapshot the updated model snapshot object to be stored
     * @param restoreModelSizeStats should the model size stats in this
     * snapshot be made the current ones for this job?
     * @throws UnknownJobException If there is no job with id <code>jobId</code>
     */
    public void updateModelSnapshot(String jobId, ModelSnapshot modelSnapshot,
                                    boolean restoreModelSizeStats) throws UnknownJobException;

    /**
     * Delete a persisted model snapshot.
     *
     * @param jobId the job ID
     * @param snapshotId the ID of the snapshot to be deleted
     * @throws UnknownJobException If there is no job with ID <code>jobId</code>
     * @throws NoSuchModelSnapshotException If there is no snapshot with ID <code>snapshotId</code> for the job
     */
    public ModelSnapshot deleteModelSnapshot(String jobId, String snapshotId)
            throws UnknownJobException, NoSuchModelSnapshotException;


    /**
     * Get the job's model size stats.
     *
     * @param jobId
     * @return
     */
    public Optional<ModelSizeStats> modelSizeStats(String jobId);

    /**
     * Refresh the datastore index so that all recent changes are
     * available to search operations. This is a synchronous blocking
     * call that should not return until the index has been refreshed.
     *
     * @param jobId
     */
    public void refreshIndex(String jobId);

    /**
     * Get an auditor for the given job
     *
     * @param jobId the job id
     * @return the {@code Auditor}
     */
    Auditor audit(String jobId);
}
