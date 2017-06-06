/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.xpack.ml.MlMetadata;

/**
 * Methods for handling index naming related functions
 */
public final class AnomalyDetectorsIndex {

    public static final String RESULTS_INDEX_PREFIX = ".ml-anomalies-";
    private static final String STATE_INDEX_NAME = ".ml-state";
    public static final String RESULTS_INDEX_DEFAULT = "shared";

    private AnomalyDetectorsIndex() {
    }

    public static String jobResultsIndexPrefix() {
        return RESULTS_INDEX_PREFIX;
    }

    /**
     * The name of the alias pointing to the indices where the job's results are stored
     * @param jobId Job Id
     * @return The read alias
     */
    public static String jobResultsAliasedName(String jobId) {
        return RESULTS_INDEX_PREFIX + jobId;
    }

    /**
     * The name of the alias pointing to the write index for a job
     * @param jobId Job Id
     * @return The write alias
     */
    public static String resultsWriteAlias(String jobId) {
        // ".write" rather than simply "write" to avoid the danger of clashing
        // with the read alias of a job whose name begins with "write-"
        return RESULTS_INDEX_PREFIX + ".write-" + jobId;
    }

    /**
     * Retrieves the currently defined physical index from the job state
     * @param jobId Job Id
     * @return The index name
     */
    public static String getPhysicalIndexFromState(ClusterState state, String jobId) {
        MlMetadata meta = state.getMetaData().custom(MlMetadata.TYPE);
        return meta.getJobs().get(jobId).getResultsIndexName();
    }

    /**
     * The name of the default index where a job's state is stored
     * @return The index name
     */
    public static String jobStateIndexName() {
        return STATE_INDEX_NAME;
    }
}
