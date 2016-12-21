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
package org.elasticsearch.xpack.prelert.scheduler;

import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public final class ScheduledJobValidator {

    private ScheduledJobValidator() {}

    /**
     * Validates a schedulerConfig in relation to the job it refers to
     * @param schedulerConfig the scheduler config
     * @param job the job
     */
    public static void validate(SchedulerConfig schedulerConfig, Job job) {
        AnalysisConfig analysisConfig = job.getAnalysisConfig();
        if (analysisConfig.getLatency() != null && analysisConfig.getLatency() > 0) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.SCHEDULER_DOES_NOT_SUPPORT_JOB_WITH_LATENCY));
        }
        if (schedulerConfig.hasAggregations() && !SchedulerConfig.DOC_COUNT.equals(analysisConfig.getSummaryCountFieldName())) {
            throw new IllegalArgumentException(
                    Messages.getMessage(Messages.SCHEDULER_AGGREGATIONS_REQUIRES_JOB_WITH_SUMMARY_COUNT_FIELD, SchedulerConfig.DOC_COUNT));
        }
        DataDescription dataDescription = job.getDataDescription();
        if (dataDescription == null || dataDescription.getFormat() != DataDescription.DataFormat.ELASTICSEARCH) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.SCHEDULER_REQUIRES_JOB_WITH_DATAFORMAT_ELASTICSEARCH));
        }
    }
}
