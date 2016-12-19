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
package org.elasticsearch.xpack.prelert.job.process.normalizer;

import java.util.concurrent.ExecutorService;

/**
 * Factory interface for creating implementations of {@link NormalizerProcess}
 */
public interface NormalizerProcessFactory {
    /**
     *  Create an implementation of {@link NormalizerProcess}
     *
     * @param executorService Executor service used to start the async tasks a job needs to operate the analytical process
     * @return The process
     */
    NormalizerProcess createNormalizerProcess(String jobId, String quantilesState, Integer bucketSpan, boolean perPartitionNormalization,
                                              ExecutorService executorService);
}
