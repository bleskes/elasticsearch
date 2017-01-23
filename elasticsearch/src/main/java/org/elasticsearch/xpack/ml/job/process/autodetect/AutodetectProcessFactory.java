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
package org.elasticsearch.xpack.ml.job.process.autodetect;

import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;
import org.elasticsearch.xpack.ml.job.config.ListDocument;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Factory interface for creating implementations of {@link AutodetectProcess}
 */
public interface AutodetectProcessFactory {

    /**
     * Create an implementation of {@link AutodetectProcess}
     *
     * @param job             Job configuration for the analysis process
     * @param modelSnapshot   The model snapshot to restore from
     * @param quantiles       The quantiles to push to the native process
     * @param list            The lists to push to the native process
     * @param ignoreDowntime  Should gaps in data be treated as anomalous or as a maintenance window after a job re-start
     * @param executorService Executor service used to start the async tasks a job needs to operate the analytical process
     * @return The process
     */
    AutodetectProcess createAutodetectProcess(Job job, ModelSnapshot modelSnapshot, Quantiles quantiles, Set<ListDocument> list,
                                              boolean ignoreDowntime, ExecutorService executorService);
}
