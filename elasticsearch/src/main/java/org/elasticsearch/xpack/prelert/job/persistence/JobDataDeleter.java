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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;

public interface JobDataDeleter {

    /**
     * Delete all result types (Buckets, Records, Influencers) from {@code cutOffTime}
     *
     * @param cutoffEpochMs Results at and after this time will be deleted
     */
    void deleteResultsFromTime(long cutoffEpochMs);

    /**
     * Delete a {@code ModelSnapshot}
     *
     * @param modelSnapshot the model snapshot to delete
     */
    void deleteModelSnapshot(ModelSnapshot modelSnapshot);

    /**
     * Delete a {@code ModelDebugOutput} record
     *
     * @param modelDebugOutput to delete
     */
    void deleteModelDebugOutput(ModelDebugOutput modelDebugOutput);

    /**
     * Delete a {@code ModelSizeStats} record
     *
     * @param modelSizeStats to delete
     */
    void deleteModelSizeStats(ModelSizeStats modelSizeStats);

    /**
     * Delete all results marked as interim
     */
    void deleteInterimResults();

    /**
     * Commit the deletions without enforcing the removal of data from disk
     */
    void commit(ActionListener<BulkResponse> listener);
}
