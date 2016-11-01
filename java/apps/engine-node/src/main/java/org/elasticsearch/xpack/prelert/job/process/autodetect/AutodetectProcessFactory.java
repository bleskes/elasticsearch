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
