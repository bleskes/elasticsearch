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
    private static final String RESULTS_INDEX_PREFIX = ".ml-anomalies-";
    private static final String STATE_INDEX_NAME = ".ml-state";

    private AnomalyDetectorsIndex() {
    }

    public static String jobResultsIndexPrefix() {
        return RESULTS_INDEX_PREFIX;
    }

    /**
     * The name of the default index where the job's results are stored
     * @param jobId Job Id
     * @return The index name
     */
    public static String jobResultsIndexName(String jobId) {
        return RESULTS_INDEX_PREFIX + jobId;
    }

    /**
     * The default index pattern for rollover index results
     * @param jobId Job Id
     * @return The index name
     */
    public static String getCurrentResultsIndex(ClusterState state, String jobId) {
        MlMetadata meta = state.getMetaData().custom(MlMetadata.TYPE);
        return RESULTS_INDEX_PREFIX + meta.getJobs().get(jobId).getResultsIndexName();
    }

    /**
     * The name of the default index where a job's state is stored
     * @return The index name
     */
    public static String jobStateIndexName() {
        return STATE_INDEX_NAME;
    }
}
