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
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * Methods for handling index naming related functions
 */
public final class AnomalyDetectorsIndex {
    private static final String RESULTS_INDEX_PREFIX = "prelertresults-";
    private static final String STATE_INDEX_NAME = ".ml-state";

    private AnomalyDetectorsIndex() {
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
     * The name of the default index where a job's state is stored
     * @return The index name
     */
    public static String jobStateIndexName() {
        return STATE_INDEX_NAME;
    }
}
