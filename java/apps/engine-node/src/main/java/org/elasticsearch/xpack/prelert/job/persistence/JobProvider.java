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


import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.lists.ListDocument;

import java.util.Optional;

public interface JobProvider extends JobDetailsProvider, JobResultsProvider {
    /**
     * Return true if the data store is accessible for the given job Id
     */
    boolean isConnected(String jobId);

    /**
     * Get the persisted quantiles state for the job
     */
    Optional<Quantiles> getQuantiles(String jobId);

    /**
     * Get model snapshots for the job ordered by descending restore priority.
     *
     * @param jobId the job id
     * @param skip  number of snapshots to skip
     * @param take  number of snapshots to retrieve
     * @return page of model snapshots
     */
    QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take);

    /**
     * Get model snapshots for the job ordered by descending restore priority.
     *
     * @param jobId          the job id
     * @param skip           number of snapshots to skip
     * @param take           number of snapshots to retrieve
     * @param startEpochMs   earliest time to include (inclusive)
     * @param endEpochMs     latest time to include (exclusive)
     * @param sortField      optional sort field name (may be null)
     * @param sortDescending Sort in descending order
     * @param snapshotId     optional snapshot ID to match (null for all)
     * @param description    optional description to match (null for all)
     * @return page of model snapshots
     */
    QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take,
            String startEpochMs, String endEpochMs, String sortField, boolean sortDescending,
            String snapshotId, String description);

    /**
     * Update a persisted model snapshot metadata document to match the
     * argument supplied.
     *
     * @param jobId                 the job id
     * @param modelSnapshot         the updated model snapshot object to be stored
     * @param restoreModelSizeStats should the model size stats in this
     *                              snapshot be made the current ones for this job?
     */
    void updateModelSnapshot(String jobId, ModelSnapshot modelSnapshot,
            boolean restoreModelSizeStats);

    /**
     * Delete a persisted model snapshot.
     *
     * @param jobId      the job ID
     * @param snapshotId the ID of the snapshot to be deleted
     */
    ModelSnapshot deleteModelSnapshot(String jobId, String snapshotId);


    /**
     * Get the job's model size stats.
     */
    Optional<ModelSizeStats> modelSizeStats(String jobId);

    /**
     * Retrieves the list with the given {@code listId} from the datastore.
     *
     * @param listId the id of the requested list
     * @return the matching list if it exists
     */
    Optional<ListDocument> getList(String listId);

    /**
     * Refresh the datastore index so that all recent changes are
     * available to search operations. This is a synchronous blocking
     * call that should not return until the index has been refreshed.
     */
    void refreshIndex(String jobId);

    /**
     * Get an auditor for the given job
     *
     * @param jobId the job id
     * @return the {@code Auditor}
     */
    Auditor audit(String jobId);
}
